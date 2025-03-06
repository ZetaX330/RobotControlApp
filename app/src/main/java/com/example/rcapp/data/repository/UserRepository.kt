package com.example.rcapp.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.example.rcapp.data.database.UserDatabaseHelper
import com.example.rcapp.data.network.RetrofitClient.apiService
import com.example.rcapp.model.FeedbackMetadata
import com.example.rcapp.model.LoginResponse
import com.example.rcapp.model.MediaItem
import com.example.rcapp.model.MediaType
import com.example.rcapp.model.User
import com.example.rcapp.model.toMimeType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import okhttp3.ResponseBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class UserRepository(private val context: Context) {
    fun saveUserInfo(user: User) {

        UserDatabaseHelper(context).createUser(user.phone)

        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt("id", user.id)
            putString("name", user.name)
            putString("phone", user.phone)
            putString("email", user.email)
            putFloat("balance", user.balance.toFloat())
            putBoolean("isLoggedIn", true) // 添加自动登录标记
            apply()
        }
        val directory = File(context.filesDir, user.phone)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val avatarFile = File(directory, "avatar.png")
        val decodedBytes = Base64.decode(user.avatarBase64, Base64.DEFAULT)
        FileOutputStream(avatarFile).use { output ->
            output.write(decodedBytes)
        }
    }

    fun login(phone: String, password: String, callback: (Boolean, String) -> Unit) {
        val requestBody = mapOf("phone" to phone, "password" to password)
        val call = apiService.login(requestBody)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        saveUserInfo(loginResponse.user)
                        callback(true,loginResponse.message)
                    }
                }
                else {
                    response.errorBody()?.let { errorBody ->
                        val errorMessage = try {
                            val errorJson = JSONObject(errorBody.string())
                            errorJson.optString("error", "未知错误，请稍后再试")
                        } catch (e: Exception) {
                            "未知错误，请稍后再试"
                        }
                        callback(false, errorMessage)
                    }
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                callback(false,"Network error: ${t.message}")
            }
        })
    }

    /**
     * 实现apiService接口
     * enqueue 方法用于将网络请求加入队列，并在后台线程中异步执行，不会阻塞主线程
     */
    fun register(user: User, callback: (Boolean, String) -> Unit) {
        val call = apiService.register(user)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    callback(true, "注册成功")
                }
                else {
                    callback(false, "注册失败")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback(false, "网络错误")
            }
        })
    }

    fun modifyAvatar(phone: String,avatarBase64:String,callback: (Boolean, String) -> Unit) {
        val requestBody = mapOf("phone" to phone,"avatar_base64" to avatarBase64)
        val call = apiService.modifyAvatar(requestBody)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    callback(true, "修改成功")
                }
                else {
                    callback(false, "修改失败")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback(false, "网络错误")
            }
        })
    }
    // 确保添加 Context 参数以访问 ContentResolver
    fun submitFeedback(
        context: Context,
        feedback: FeedbackMetadata,
        mediaItems: MutableList<MediaItem?>,
        callback: (Boolean, String) -> Unit
    ) {
        // 1. 序列化 FeedbackMetadata
        val feedbackPart = try {
            Json.encodeToString(feedback).toRequestBody("application/json".toMediaType())
        } catch (e: Exception) {
            callback(false, "序列化失败: ${e.message}")
            return
        }

        // 2. 处理媒体文件
        val mediaParts = mediaItems.mapNotNull { mediaItem ->
            mediaItem?.let { item ->
                try {
                    // 检查 URI 权限
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    try {
                        context.contentResolver.takePersistableUriPermission(item.uri, takeFlags)
                    } catch (e: SecurityException) {
                        callback(false, "无权限访问文件: ${item.fileName}")
                        return@mapNotNull null
                    }

                    // 创建 Part（延迟打开流）
                    MultipartBody.Part.createFormData(
                        "media",
                        item.fileName,
                        createStreamRequestBody(context, item)
                    )
                } catch (e: Exception) {
                    callback(false, "文件处理失败: ${item.fileName}, ${e.message}")
                    null
                }
            }
        }

        // 3. 发送请求
        val call = apiService.submitFeedback(feedbackPart, mediaParts)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                callback(true, "提交成功")
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback(false, "网络错误: ${t.message ?: "未知原因"}")
            }
        })
    }

    private fun createStreamRequestBody(context: Context, item: MediaItem): RequestBody {
        return object : RequestBody() {
            override fun contentType() = item.type.toMimeType().toMediaTypeOrNull()
            override fun contentLength() = item.fileSize

            override fun writeTo(sink: BufferedSink) {
                context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    sink.writeAll(stream.source())
                } ?: throw IOException("无法打开文件: ${item.fileName}")
            }
        }
    }


    companion object {


    }
}