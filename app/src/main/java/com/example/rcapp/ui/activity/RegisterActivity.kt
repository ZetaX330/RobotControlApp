package com.example.rcapp.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.R
import com.example.rcapp.databinding.ActivityRegisterBinding
import com.example.rcapp.model.User
import com.example.rcapp.data.repository.UserRepository
import com.example.rcapp.ui.viewmodel.RegisterViewModel
import com.example.rcapp.ui.viewmodel.RepositoryViewModelFactory

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding :ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel = ViewModelProvider(this, RepositoryViewModelFactory(userRepository))[RegisterViewModel::class.java]
        setRegisterListeners()

    }
    private fun setRegisterListeners(){
        viewModel.registerResult.observe(this) { result ->
            val (success, message) = result
            if(success){
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                //startActivity(Intent(this,MainActivity::class.java))
            }
        }
        binding.registerSubmitBtn.setOnClickListener {
            val account = binding.registerAccountEt.text.toString()
            val password = binding.registerPasswordEt.text.toString()
            val phone = binding.registerPhoneEt.text.toString()
            val gender = 1
            val email = binding.registerEmailBoxEt.text.toString()
            val user = User(null, null, account, password, phone, gender, email)
            viewModel.register(user)
        }
    }
    private fun contentCheck(){

    }
}