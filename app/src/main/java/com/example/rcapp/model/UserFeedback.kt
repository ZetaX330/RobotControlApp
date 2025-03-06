package com.example.rcapp.model

import android.net.Uri
import com.example.rcapp.util.UriSerializer
import kotlinx.serialization.Serializable
import java.util.UUID
@Serializable
data class FeedbackMetadata(
    val feedbackId: String = UUID.randomUUID().toString(),
    val phone: String,
    val feedbackText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class MediaItem(
    @Serializable(with = UriSerializer::class)
    val uri: Uri,
    val type: MediaType,
    val fileName: String,
    val fileSize: Long
){

    fun isVideoValid(): Boolean {
        return fileSize <= MAX_VIDEO_SIZE_BYTES
    }
}

@Serializable
enum class MediaType { IMAGE, VIDEO }
// 辅助扩展：将 MediaType 转换为 MIME 类型
fun MediaType.toMimeType(): String = when (this) {
    MediaType.IMAGE -> "image/*"
    MediaType.VIDEO -> "video/*"
}
// 50MB 的严格字节数（二进制计算）
private const val MAX_VIDEO_SIZE_BYTES = 50 * 1024 * 1024 // 52,428,800 字节
