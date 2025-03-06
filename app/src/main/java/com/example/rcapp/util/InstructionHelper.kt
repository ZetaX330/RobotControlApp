package com.example.rcapp.util

import android.util.Log
import com.example.rcapp.ui.activity.CameraActivity
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.*

object  InstructionHelper {
    // 左臂关键点索引（MediaPipe Pose 33点模型）
    private const val LEFT_SHOULDER = 11
    private const val LEFT_ELBOW = 13
    private const val LEFT_WRIST = 15
    fun instructionCreate(poseLandmarkerResults: PoseLandmarkerResult): String {
        // 检查是否存在有效检测结果
        if (poseLandmarkerResults.landmarks().isEmpty()) {
            //Log.d("CameraActivity","landmarks().isEmpty")
            return ""
        }

        // 获取第一个检测到的人体关键点（索引0）
        val landmarks = poseLandmarkerResults.landmarks()[0]



        // 提取关键点（带空安全校验）
        val shoulder = landmarks.getOrNull(LEFT_SHOULDER) ?: return ""
        val elbow = landmarks.getOrNull(LEFT_ELBOW) ?: return ""
        val wrist = landmarks.getOrNull(LEFT_WRIST) ?: return ""
        // ✅ 0.10.20 新增 presence 检查（推荐阈值：visibility≥0.3，presence≥0.5）
        // ✅ 处理 Optional<Float> 类型的 visibility()
        if (!shoulder.visibility().isPresent || !elbow.visibility().isPresent || !wrist.visibility().isPresent) {
            //Log.d("CameraActivity","!isPresent")
            return "" // 存在未检测到的关键点
        }

        // 解包 Optional 值
        val shoulderVis = shoulder.visibility().get()
        val elbowVis = elbow.visibility().get()
        val wristVis = wrist.visibility().get()

        // 可见性阈值检查
//        if (shoulderVis < 0.3f || elbowVis < 0.3f || wristVis < 0.3f) {
//            Log.d("CameraActivity","!Vis")
//            return ""
//        }

        // 计算左肘关节角度
        val angle = calculateVerticalAngle(elbow, wrist)

        // 将角度编码到返回字符串（示例：角度120° → "A120B..."）
        return angle.toString()
    }

    private fun calculateVerticalAngle(
        elbow: NormalizedLandmark,
        wrist: NormalizedLandmark
    ): Double {
        // 小臂向量（肘部 → 腕部）
        val forearmVector = arrayOf(
            wrist.x() - elbow.x(),  // X分量
            wrist.y() - elbow.y()   // Y分量（注意：图像坐标系Y轴向下）
        )

        // 垂直参考向量（Y轴负方向，因图像坐标系Y向下）
        val verticalVector = arrayOf(0.0f, -1.0f)

        // 计算夹角（0°-180°）
        return calculateAngleBetweenVectors(forearmVector, verticalVector)
    }

    /**
     * 计算两个2D向量之间的角度
     */
    private fun calculateAngleBetweenVectors(
        vec1: Array<Float>,
        vec2: Array<Float>
    ): Double {
        // 点积
        val dotProduct = vec1[0] * vec2[0] + vec1[1] * vec2[1]

        // 模长
        val mag1 = sqrt(vec1[0].pow(2) + vec1[1].pow(2))
        val mag2 = sqrt(vec2[0].pow(2) + vec2[1].pow(2))

        // 弧度 → 角度
        var angle = Math.toDegrees(acos(dotProduct / (mag1 * mag2)).toDouble())

        // 处理方向（通过叉积符号确定角度方向）
        val crossProduct = vec1[0] * vec2[1] - vec1[1] * vec2[0]
        if (crossProduct < 0) angle = 360 - angle

        // 标准化到0-180°
        return if (angle > 180) 360 - angle else angle
    }


    /** 将角度编码到指定格式 */
    private fun encodeAngleToFormat(angle: Double): String {
        val code = angle.toInt().toString().padStart(3, '0') // 角度转3位数字
        return "A${code}B2C3D4E5F6A7B8" // 示例编码逻辑（根据需求修改）
    }

}