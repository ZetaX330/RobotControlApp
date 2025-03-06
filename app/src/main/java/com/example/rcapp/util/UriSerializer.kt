package com.example.rcapp.util

import android.net.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UriSerializer : KSerializer<Uri> {
    // 序列化描述符（字符串类型）
    override val descriptor = PrimitiveSerialDescriptor("AndroidUri", PrimitiveKind.STRING)

    // 序列化：将 Uri 转换为字符串
    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }

    // 反序列化：从字符串重建 Uri
    override fun deserialize(decoder: Decoder): Uri {
        val uriString = decoder.decodeString()
        return parseUri(uriString)
    }

    // 安全解析 Uri（带校验逻辑）
    private fun parseUri(uriString: String): Uri {
        return Uri.parse(uriString).takeIf { isValidUri(it) }
            ?: throw IllegalArgumentException("Invalid Uri format: $uriString")
    }

    // 校验 Uri 有效性
    private fun isValidUri(uri: Uri): Boolean {
        return when (uri.scheme) {
            "content", "file", "http", "https" -> true
            else -> false
        }
    }
}