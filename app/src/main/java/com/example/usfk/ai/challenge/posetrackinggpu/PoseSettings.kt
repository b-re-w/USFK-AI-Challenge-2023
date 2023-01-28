package com.example.usfk.ai.challenge.posetrackinggpu


import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList
import kotlin.math.*


enum class KeyPoints {
    NOSE, LEFT_EYE_INNER, LEFT_EYE, LEFT_EYE_OUTER, RIGHT_EYE_INNER, RIGHT_EYE, RIGHT_EYE_OUTER, LEFT_EAR,
    RIGHT_EAR, MOUTH_LEFT, MOUTH_RIGHT, LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_ELBOW, RIGHT_ELBOW, LEFT_WRIST, RIGHT_WRIST,
    LEFT_PINKY, RIGHT_PINKY, LEFT_INDEX, RIGHT_INDEX, LEFT_THUMB, RIGHT_THUMB, LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE, LEFT_ANKLE, RIGHT_ANKLE, LEFT_HEEL, RIGHT_HEEL, LEFT_FOOT_INDEX, RIGHT_FOOT_INDEX
}

object ArmAnglesRecipe {
    fun toList(): List<List<KeyPoints>> = listOf(rightElbow, leftElbow, rightShoulder, leftShoulder)

    fun toMap(): Map<String, List<KeyPoints>> = mapOf("right elbow" to rightElbow, "left elbow" to leftElbow,
        "right shoulder" to rightShoulder, "left shoulder" to leftShoulder)

    val rightElbow = listOf(
        KeyPoints.RIGHT_SHOULDER, KeyPoints.RIGHT_ELBOW, KeyPoints.RIGHT_WRIST)

    val leftElbow = listOf(
        KeyPoints.LEFT_SHOULDER, KeyPoints.LEFT_ELBOW, KeyPoints.LEFT_WRIST)

    val rightShoulder = listOf(
        KeyPoints.RIGHT_HIP, KeyPoints.RIGHT_SHOULDER, KeyPoints.RIGHT_ELBOW)

    val leftShoulder = listOf(
        KeyPoints.LEFT_HIP, KeyPoints.LEFT_SHOULDER, KeyPoints.LEFT_ELBOW)
}

object LegAnglesRecipe {
    fun toList(): List<List<KeyPoints>> = listOf(rightKnee, leftKnee, rightAnkle, leftAnkle)

    fun toMap(): Map<String, List<KeyPoints>> = mapOf("right knee" to rightKnee, "left knee" to leftKnee,
        "right ankle" to rightAnkle, "left ankle" to leftAnkle)

    val rightKnee = listOf(
        KeyPoints.RIGHT_HIP, KeyPoints.RIGHT_KNEE, KeyPoints.RIGHT_ANKLE)

    val leftKnee = listOf(
        KeyPoints.LEFT_HIP, KeyPoints.LEFT_KNEE, KeyPoints.LEFT_ANKLE)

    val rightAnkle = listOf(
        KeyPoints.RIGHT_KNEE, KeyPoints.RIGHT_ANKLE, KeyPoints.RIGHT_FOOT_INDEX)

    val leftAnkle = listOf(
        KeyPoints.LEFT_KNEE, KeyPoints.LEFT_ANKLE, KeyPoints.LEFT_FOOT_INDEX)
}

object BodyAnglesRecipe {
    fun toList(): List<List<KeyPoints>> = listOf(rightHipKnee, leftHipKnee, rightHipFoot, leftHipFoot)

    fun toMap(): Map<String, List<KeyPoints>> = mapOf("right hip to right knee" to rightHipKnee,
        "left hip to left knee" to leftHipKnee, "right hip to right foot" to rightHipFoot, "left hip to left foot" to leftHipFoot)

    val rightHipKnee = listOf(
        KeyPoints.RIGHT_SHOULDER, KeyPoints.RIGHT_HIP, KeyPoints.RIGHT_KNEE)

    val leftHipKnee = listOf(
        KeyPoints.LEFT_SHOULDER, KeyPoints.LEFT_HIP, KeyPoints.LEFT_KNEE)

    val rightHipFoot = listOf(
        KeyPoints.RIGHT_SHOULDER, KeyPoints.RIGHT_HIP, KeyPoints.RIGHT_FOOT_INDEX)

    val leftHipFoot = listOf(
        KeyPoints.LEFT_SHOULDER, KeyPoints.LEFT_HIP, KeyPoints.LEFT_FOOT_INDEX)
}


class Pose(private var data: NormalizedLandmarkList) {
    val ignoreZGlobal: Boolean = true

    fun setData(input: NormalizedLandmarkList) {
        data = input
    }

    /**
     * get angle (degree) from 3 key points
     */
    fun getAngle(triple: List<KeyPoints>, ignoreZ: Boolean = ignoreZGlobal): Double {
        val start = data.getLandmark(triple[0].ordinal)
        val middle = data.getLandmark(triple[1].ordinal)
        val end = data.getLandmark(triple[2].ordinal)
        //assert(start.visibility == 1.0f)

        if (ignoreZ) {
            val angle = (
                    (atan2(
                        end.y - middle.y, end.x - middle.x
                    ) - atan2(
                        start.y - middle.y, start.x - middle.x
                    )) * 180 / PI)
            // 60 -> 60
            // minus 60 -> ??
            // 120 -> 120
            // minus 120 -> ??
            // 180 -> 180
            // minus 180 -> ??
            // 240 -> 120
            // minus 240 -> ??
            return min(360 - abs(angle), abs(angle))
        } else {
            val v1x = start.x - middle.x
            val v1y = start.y - middle.y
            val v1z = start.z - middle.z

            val v2x = end.x - middle.x
            val v2y = end.y - middle.y
            val v2z = end.z - middle.z

            val v1mag = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
            val v1normX = v1x / v1mag
            val v1normY = v1y / v1mag
            val v1normZ = v1z / v1mag

            val v2mag = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
            val v2normX = v2x / v2mag
            val v2normY = v2y / v2mag
            val v2normZ = v2z / v2mag

            val res = v1normX * v2normX + v1normY * v2normY + v1normZ * v2normZ

            val angle = acos(res) * 180 / PI
            return min(360 - abs(angle), abs(angle))
        }
    }

    /**
     * helper function to calculate multiple angles at once
     */
    fun getAngleList(listTriples: List<List<KeyPoints>>, ignoreZ: Boolean = ignoreZGlobal): List<Double> =
        listTriples.map { triple -> getAngle(triple, ignoreZ = ignoreZ) }
}
