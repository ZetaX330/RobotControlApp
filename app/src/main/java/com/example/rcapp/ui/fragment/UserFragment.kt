package com.example.rcapp.ui.fragment

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rcapp.R
import com.example.rcapp.databinding.FragmentUserBinding
import com.example.rcapp.model.User
import com.example.rcapp.ui.activity.LoginActivity
import com.example.rcapp.ui.activity.SendMessageActivity
import com.example.rcapp.ui.activity.UserBalanceActivity
import com.example.rcapp.ui.activity.UserInformationActivity
import com.example.rcapp.ui.activity.UserFeedbackActivity
import com.example.rcapp.ui.viewmodel.UserViewModel
import java.io.File
import java.util.Locale

class UserFragment : Fragment() {
    private lateinit var binding: FragmentUserBinding
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUriString = result.data?.getStringExtra("CROPPED_IMAGE_URI")
            val resultUri = resultUriString?.let { Uri.parse(it) }
            if (resultUri != null) {
                binding.userAvatarIv.setImageURI(resultUri)
            }
        }
    }


    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.white)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUserInfo()
        setUserListeners()

    }
    private fun setUserInfo(){
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val name = sharedPreferences.getString("name", null)
        val phone = sharedPreferences.getString("phone", null)
        val email = sharedPreferences.getString("email", null)
        val balance = sharedPreferences.getFloat("balance", 0.00f)
        if (name != null && phone != null) {
            val user=User(
                name=name,
                phone=phone,
                balance = balance.toBigDecimal())
            val directory = File(context?.filesDir, user.phone)
            val avatarFile = File(directory, "avatar.png")
            val bitmap = BitmapFactory.decodeFile(avatarFile.absolutePath)
            binding.run {
                userAvatarIv.setImageBitmap(bitmap)
                userNameTv.text=user.name
                userPhoneTv.text=user.phone
                userBalanceTv.text= String.format(Locale.CHINA, "%.2f￥", balance)
            }

        }
    }
    private fun setUserListeners(){
        binding.run {
            userAvatarIv.setOnClickListener {
                val intent = Intent(context, UserInformationActivity::class.java)
                imagePickerLauncher.launch(intent)
            }
            userFeedbackIv.setOnClickListener {
                startActivity(Intent(context,UserFeedbackActivity::class.java))

            }
            userBalance.setOnClickListener {
                startActivity(Intent(context,UserBalanceActivity::class.java))
            }

            userExitIv.setOnClickListener {
                val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear() // 清空所有数据
                editor.apply() // 提交更改
                // 2. 跳转到登录界面
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                // 3. 结束当前 Activity
                requireActivity().finish()
            }
        }

    }
}