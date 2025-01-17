package com.example.rcapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rcapp.R
import com.example.rcapp.databinding.ActivityLoginBinding
import com.example.rcapp.ui.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        val userRepository = UserRepository()
//        viewModel = ViewModelProvider(this, RepositoryViewModelFactory(userRepository))[LoginViewModel::class.java]
//        viewModel.loginResult.observe(this) { result ->
//            val (success, message) = result
//            if(success){
//                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//                startActivity(Intent(this, MainActivity::class.java))
//            }
//        }

        setLoginListeners()
    }
    private fun setLoginListeners() {
        binding.loginBtn.setOnClickListener {
            val phone = binding.loginAccountEt.text.toString()
//            viewModel.login(username, password)
        }
        binding.loginModelChangeTv.setOnClickListener{
            if(binding.loginModelPassword.visibility== View.VISIBLE){
                binding.loginModelPassword.visibility= View.GONE
                binding.loginModelVerification.visibility= View.VISIBLE
            }
            else{
                binding.loginModelPassword.visibility= View.VISIBLE
                binding.loginModelVerification.visibility= View.GONE
            }

        }
        binding.loginRegisterTv.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}