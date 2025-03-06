package com.example.rcapp.data.network

import com.example.rcapp.config.ServerConfig
import com.example.rcapp.model.LoginResponse
import com.example.rcapp.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import kotlin.io.encoding.Base64

/**
 * @Body注解表示user对象将被序列化并作为HTTP请求的body发送
 * Call是Retrofit用于封装HTTP请求的类型，ResponseBody表示我们期望从服务器接收原始响应体
 */
interface ApiService {
    @POST(ServerConfig.LOGIN_ENDPOINT)
    fun login(@Body requestBody: Map<String, String>): Call<LoginResponse>
    @POST(ServerConfig.REGISTER_ENDPOINT)
    fun register(@Body user: User): Call<ResponseBody>
    @POST(ServerConfig.MODIFY_AVATAR_ENDPOINT)
    fun modifyAvatar(@Body requestBody:Map<String, String>): Call<ResponseBody>
    // 修改后接口（正确）
    @Multipart // 关键：启用自动构建 Multipart
    @POST(ServerConfig.SUBMIT_FEEDBACK_ENDPOINT)
    fun submitFeedback(
        @Part("feedback") feedbackPart: RequestBody,          // 元数据部分
        @Part mediaParts: List<MultipartBody.Part>            // 文件部分
    ): Call<ResponseBody>
}