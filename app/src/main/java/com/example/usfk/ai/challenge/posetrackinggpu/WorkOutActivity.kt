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
import android.widget.TextView
import androidx.core.view.WindowCompat
import com.example.usfk.ai.challenge.R
import com.google.mediapipe.apps.basic.BasicActivity
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.protobuf.InvalidProtocolBufferException

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

    override fun onCreate(savedInstanceState: Bundle?) {
        setStatusBarTransparent()
        super.onCreate(savedInstanceState)
        poseTextView = findViewById(R.id.textView)

        // To show verbose logging, run:
        // adb shell setprop log.tag.MainActivity VERBOSE
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            processor.addPacketCallback(OUTPUT_LANDMARKS_STREAM_NAME) { packet: Packet ->
                Log.v(TAG, "Received pose landmarks packet.")
                try {
                    val landmarksRaw = PacketGetter.getProtoBytes(packet)
                    val poseLandmarks = NormalizedLandmarkList.parseFrom(landmarksRaw)
                    val logs = getPoseLandmarksDebugString(poseLandmarks)
                    poseTextView.text = logs
                    //Toast.makeText(this, logs, Toast.LENGTH_SHORT).show();
                    Log.v(TAG, "[TS:" + packet.timestamp + "] " + logs)
                } catch (exception: InvalidProtocolBufferException) {
                    Log.e(TAG, "Failed to get proto.", exception)
                }
            }
        }
    }

    private fun getPoseLandmarksDebugString(poseLandmarks: NormalizedLandmarkList): String {
        var poseLandmarkStr = """
                Pose landmarks: ${poseLandmarks.landmarkCount}
                
                """.trimIndent()
        var landmarkIndex = 0
        for (landmark in poseLandmarks.landmarkList) {
            poseLandmarkStr += ("\tLandmark ["
                    + landmarkIndex
                    + "]: ("
                    + landmark.x
                    + ", "
                    + landmark.y
                    + ", "
                    + landmark.z
                    + ")\n")
            ++landmarkIndex
        }
        return poseLandmarkStr
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