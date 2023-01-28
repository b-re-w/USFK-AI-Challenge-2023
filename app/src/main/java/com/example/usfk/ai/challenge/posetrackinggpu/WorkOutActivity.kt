// Copyright 2020 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.usfk.ai.challenge.posetrackinggpu

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.view.WindowCompat
import com.example.usfk.ai.challenge.R
import com.google.mediapipe.apps.basic.BasicActivity
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.protobuf.InvalidProtocolBufferException
import kotlin.math.*

/** Main activity of MediaPipe pose tracking app.  */
class WorkOutActivity : BasicActivity() {
    companion object {
        private const val TAG = "WorkOutActivity"
        private const val OUTPUT_LANDMARKS_STREAM_NAME = "pose_landmarks"
    }

    override fun getContentViewLayoutResId(): Int {
        Log.i(TAG, "Current ContentView Layout is Activity_Workout")
        return R.layout.activity_workout
    }

    private lateinit var poseTextView: TextView
    private lateinit var startButton: Button
    private lateinit var poseAnalyzer: Pose
    private val recipeMap = ArmAnglesRecipe.toMap() + LegAnglesRecipe.toMap() + BodyAnglesRecipe.toMap()
    private val recipeList = recipeMap.toList().map { it.second }
    private val recipeName = recipeMap.toList().map { it.first }

    private var isWorkOutStarted = false
    private var count = 0
    private var squatCriteria = listOf(77.33, 112.42, 115.12)  // knee, ankle, right hip to right knee
    private var normalSituation = listOf(160.67, 150.15, 156.32)  // knee, ankle, right hip to right knee
    private var threadsHold = 98.9
    private var hitCriteria = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setStatusBarTransparent()
        super.onCreate(savedInstanceState)
        poseTextView = findViewById(R.id.pose_text)
        findViewById<Button>(R.id.flip_button)?.setOnClickListener { flipCamera() }
        startButton = findViewById(R.id.squat_button)
        startButton.setOnClickListener { startSquat() }

        // To show verbose logging, run:
        // adb shell setprop log.tag.MainActivity VERBOSE
        processor.addPacketCallback(OUTPUT_LANDMARKS_STREAM_NAME) { packet: Packet ->
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "Received pose landmarks packet.")
            try {
                val landmarksRaw = PacketGetter.getProtoBytes(packet)
                val poseLandmarks = NormalizedLandmarkList.parseFrom(landmarksRaw)
                //var logs = getPoseLandmarksDebugString(poseLandmarks)
                if (!::poseAnalyzer.isInitialized) {
                    poseAnalyzer = Pose(poseLandmarks)
                } else {
                    poseAnalyzer.setData(poseLandmarks)
                }
                val anal = poseAnalyzer.getAngleList(recipeList)
                if (isWorkOutStarted) {
                    // Euclidean Distance (MAX Dis 623.54)
                    val currentRight = listOf(anal[4], anal[6], anal[8])
                    val currentLeft = listOf(anal[5], anal[7], anal[9])
                    var rightPercent = 0.0
                    var leftPercent = 0.0
                    if (!hitCriteria) {
                        rightPercent = getPercent(getEuclideanDistance(squatCriteria, currentRight))
                        leftPercent = getPercent(getEuclideanDistance(squatCriteria, currentLeft))
                        if (rightPercent >= threadsHold && leftPercent >= threadsHold) {
                            hitCriteria = true
                        }
                    } else {
                        rightPercent = getPercent(getEuclideanDistance(normalSituation, currentRight))
                        leftPercent = getPercent(getEuclideanDistance(normalSituation, currentLeft))
                        if (rightPercent >= threadsHold && leftPercent >= threadsHold) {
                            hitCriteria = false
                            count++
                        }
                    }
                    poseTextView.text = "<Next Posture> ${if (!hitCriteria) "Squat Down" else "Squat Up"}\n(R)Posture Correspondance: ${"%.2f".format(rightPercent)}\n(L)Posture Correspondance: ${"%.2f".format(leftPercent)}\nSquat Count: $count"
                } else {
                    poseTextView.text = anal.mapIndexed { index, value ->
                        "${recipeName[index]}:  ${"%.2f".format(value)}${if (index % 2 == 0 && index < 8) "  |  " else "\n"}"
                    }.joinToString("")
                }
                //poseTextView.text = logs.replace("\n\n", " ")
                //logs = logs.replace("\n\n", "\n")
                //if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "[TS:" + packet.timestamp + "] " + logs)
            } catch (exception: InvalidProtocolBufferException) {
                Log.e(TAG, "Failed to get proto.", exception)
            }
        }
    }

    private fun getEuclideanDistance(criteria: List<Double>, current: List<Double>) =
        sqrt((criteria[0]-current[0]).pow(2) + (criteria[1]-current[1]).pow(2) + (criteria[2]-current[2]).pow(2))

    private fun getPercent(distance: Double): Double = (623.54 - distance) / 6.2354

    private fun getPoseLandmarksDebugString(poseLandmarks: NormalizedLandmarkList): String {
        var poseLandmarkStr = """
                Pose landmarks: ${poseLandmarks.landmarkCount}
                
                """.trimIndent()
        for ((landmarkIndex, landmark) in poseLandmarks.landmarkList.withIndex()) {
            poseLandmarkStr += ("\tLandmark ["
                    + landmarkIndex
                    + "]: ("
                    + landmark.x
                    + ", "
                    + landmark.y
                    + ", "
                    + landmark.z
                    + if (landmarkIndex % 2 == 0) ")\n\n" else ")\n")
        }
        return poseLandmarkStr
    }

    fun flipCamera() {
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            startCamera(!isCurrentCameraFacingFront)
        }
    }

    fun startSquat() {
        if (isWorkOutStarted) {
            isWorkOutStarted = false
            startButton.text = "Start Squat"
        } else {
            isWorkOutStarted = true
            startButton.text = "Stop Squat"
        }
    }

    fun setStatusBarTransparent() {
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        if(Build.VERSION.SDK_INT >= 30) {	// API 30 에 적용
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }
}