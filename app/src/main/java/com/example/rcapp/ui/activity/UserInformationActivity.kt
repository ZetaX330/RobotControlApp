package com.example.rcapp.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rcapp.data.repository.UserRepository
import com.example.rcapp.databinding.ActivityUserInformationBinding
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class UserInformationActivity : AppCompatActivity() {
    private lateinit var binding:ActivityUserInformationBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var phone:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityUserInformationBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        phone = sharedPreferences.getString("phone", null).toString()

        val avatarFile = File(File(filesDir, phone), "avatar.png")
        binding.userAvatarIv.setImageBitmap(
            BitmapFactory.decodeFile(avatarFile.absolutePath)
        )
        binding.userAvatarIv.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }
            imagePickerLauncher.launch(intent)
        }
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedImageUri = result.data?.data
                if (selectedImageUri != null) {
                    startCrop(selectedImageUri)
                }
            }
        }

    }


    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage.jpg"))
        val options = UCrop.Options().apply {
            withAspectRatio(1f, 1f) // 设置裁剪比例
            withMaxResultSize(500, 500)
        }
        UCrop.of(uri, destinationUri)
            .withOptions(options)
            .start(this)
    }

    @Deprecated("Use new method instead", ReplaceWith("newMethod()"))
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            // Convert image to Base64
            val bitmap =
                contentResolver.openInputStream(resultUri!!).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)


            UserRepository(this).modifyAvatar(phone,base64String){ success, message ->
                if(success){
                    binding.userAvatarIv.setImageBitmap(bitmap)
                    val directory = File(filesDir, phone)
                    val avatarFile = File(directory, "avatar.png")
                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                    FileOutputStream(avatarFile).use { output ->
                        output.write(decodedBytes)
                    }
                    val resultIntent = Intent().apply {
                        putExtra("CROPPED_IMAGE_URI", resultUri.toString())
                    }
                    setResult(RESULT_OK, resultIntent)
                }
                Snackbar.make(binding.root,message,Snackbar.LENGTH_SHORT).show()
            }

        }
        else if (resultCode == UCrop.RESULT_ERROR) {
//            val cropError = UCrop.getError(data!!)
        }
    }
}