package com.example.rcapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rcapp.R
import com.example.rcapp.data.repository.UserRepository
import com.example.rcapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var loginModel:Boolean=true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)

        setLoginListeners()
    }
    private fun setLoginListeners() {

        binding.loginBtn.setOnClickListener {
            if(loginModel){
                val phone = binding.loginAccountEt.text.toString()
                val password=binding.loginPasswordEt.text.toString()
                UserRepository.login(phone, password) { success, message ->
                    if(success){
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }
            }
            else{
                //验证码登录
            }

        }
        binding.loginModelChangeTv.setOnClickListener{
            if(loginModel){
                binding.loginModelChangeTv.text="密码登录"
                binding.loginModelPassword.visibility= View.GONE
                binding.loginModelVerification.visibility= View.VISIBLE
            }
            else{
                binding.loginModelChangeTv.text="验证码登录"
                binding.loginModelPassword.visibility= View.VISIBLE
                binding.loginModelVerification.visibility= View.GONE
            }
            loginModel=!loginModel

        }
        binding.loginRegisterTv.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}