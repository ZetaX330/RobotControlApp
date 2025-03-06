package com.example.rcapp.ui.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rcapp.R
import com.example.rcapp.data.repository.UserRepository
import com.example.rcapp.databinding.ActivityUserFeedbackBinding
import com.example.rcapp.model.FeedbackMetadata
import com.example.rcapp.model.MediaItem
import com.example.rcapp.model.MediaType
import com.example.rcapp.util.PreviewImageHelper
import com.google.android.material.snackbar.Snackbar
import okhttp3.Callback
import java.io.IOException
import java.util.Locale

class UserFeedbackActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserFeedbackBinding
    private var selectedIndex = 0
    // 保存三个媒体文件的变量
    private val selectedMedia = mutableListOf<MediaItem?>(null, null, null)
    private val feedbackBtn by lazy {
        listOf(
            binding.userFeedback1Btn,
            binding.userFeedback2Btn,
            binding.userFeedback3Btn
        )
    }
    private val pickImageOrVideo  = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            // 需通过变量保存当前操作的索引
            val index = selectedIndex
            setSubmitImageOrVideo(index,it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityUserFeedbackBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialToolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)
        setSupportActionBar(binding.materialToolbar)
        supportActionBar?.apply {
            title = "用户反馈"
            setDisplayHomeAsUpEnabled(true)
        }
        setListeners()
    }
    private fun setListeners() {
        binding.run {
            userFeedback1Btn.tag = 0
            userFeedback2Btn.tag = 1
            userFeedback3Btn.tag = 2

            userFeedbackTv.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    userFeedbackWordCountTv.text = String.format(Locale.CHINA, "%d/300", s?.length)
                }
            })
            userFeedbackSubmitBtn.setOnClickListener {
                submitFeedback()
            }
        }

        feedbackBtn.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedIndex = index // 保存当前索引
                pickImageOrVideo.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            }
        }
    }
    private fun setSubmitImageOrVideo(index:Int,uri: Uri) {
        try {
            // 获取文件基础信息
            val (fileName, fileSize) = getFileInfo(uri)
            val mimeType = contentResolver.getType(uri) ?: return
            // 创建媒体项基础数据
            val mediaItem = when {
                mimeType.startsWith("image/") -> {
                        MediaItem(
                            uri = uri,
                            type = MediaType.IMAGE,
                            fileName = fileName,
                            fileSize = fileSize
                        )
                    }
                mimeType.startsWith("video/") -> {
                    MediaItem(
                        uri = uri,
                        type = MediaType.VIDEO,
                        fileName = fileName,
                        fileSize = fileSize
                    )
                }
                else -> throw IllegalArgumentException("不支持的文件类型: $mimeType")
            }
            if(mediaItem.isVideoValid()){
                selectedMedia[index] = mediaItem
                when(mediaItem.type){
                    MediaType.IMAGE->{
                        loadImagePreview(index,uri)
                    }
                    MediaType.VIDEO->{
                        loadVideoPreview(index,uri)
                    }
                }
            }
        }
        catch (e: Exception) {
            //handleMediaError(index, e)
        }
    }
    // 获取文件名和大小
    private fun getFileInfo(uri: Uri): Pair<String, Long> {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                return cursor.getString(nameIndex) to cursor.getLong(sizeIndex)
            }
        }
        throw IOException("无法获取文件信息")
    }
    // 加载图片预览
    private fun loadImagePreview(index: Int,uri: Uri) {
            binding.userFeedback1Iv.visibility=View.VISIBLE
            binding.userFeedback1Iv.setImageBitmap(PreviewImageHelper.getPreviewBitmap(this,uri))
    }
    // 加载视频预览（生成首帧缩略图）
    private fun loadVideoPreview(index: Int,uri: Uri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val frame = retriever.frameAtTime
            frame?.let {
                binding.userFeedback1Iv.visibility=View.VISIBLE
                binding.userFeedback1Iv.setImageBitmap(it)
            }
        } finally {
            retriever.release()
        }
    }


//    // 错误处理
//    private fun handleMediaError(index: Int, e: Exception) {
//        Log.e("MediaUpload", "媒体处理失败", e)
//        feedbackBtn[index].setImageResource(R.drawable.ic_error)
//        Toast.makeText(this, "文件加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
//    }
    private fun submitFeedback(){
        val feedbackMetadata=FeedbackMetadata(
            phone = "15100001111",
            feedbackText= binding.userFeedbackTv.text.toString()
        )
        UserRepository(this).submitFeedback(this,feedbackMetadata,selectedMedia) { success,message ->
            if(success){
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
            else{
                Snackbar.make(binding.root,message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}