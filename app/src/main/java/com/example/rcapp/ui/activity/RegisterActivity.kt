package com.example.rcapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.R
import com.example.rcapp.databinding.ActivityRegisterBinding
import com.example.rcapp.model.User
import com.example.rcapp.data.repository.UserRepository
import com.example.rcapp.ui.viewmodel.RegisterViewModel
import com.example.rcapp.BR
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding :ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        binding.lifecycleOwner = this
        binding.viewModel = viewModel



        setRegisterListeners()

    }
    private fun setRegisterListeners(){
        binding.registerSubmitBtn.setOnClickListener {
            val name=binding.registerNameEt.text.toString()
            val phone = binding.registerPhoneEt.text.toString()
            val password = binding.registerPasswordEt.text.toString()
            val email = binding.registerEmailBoxEt.text.toString()
            val user = User(null, name, password, phone, email)
            UserRepository.register(user) { success, message ->
                if(success){
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }

        }
        binding.registerNameCheckIv.setOnClickListener {
            val message = resources.getStringArray(R.array.register_name_reminder_message)
            showSnackbar(binding.registerNameCheckIv,message)
        }
        binding.registerPhoneCheckIv.setOnClickListener {
            val message = resources.getStringArray(R.array.register_phone_reminder_message)
            showSnackbar(binding.registerPhoneCheckIv,message)
        }
        binding.registerPasswordCheckIv.setOnClickListener {
            val message = resources.getStringArray(R.array.register_password_reminder_message)
            showSnackbar(binding.registerPasswordCheckIv,message)
        }
        binding.registerPasswordConfirmCheckIv.setOnClickListener {
            val message = resources.getStringArray(R.array.register_password_reminder_message)
            showSnackbar(binding.registerPasswordConfirmCheckIv,message)
        }
        binding.registerEmailCheckIv.setOnClickListener {
            val message = resources.getStringArray(R.array.register_email_reminder_message)
            showSnackbar(binding.registerEmailCheckIv,message)
        }
    }
    private fun showSnackbar(editView: View, message:Array<String>) {
        if(editView.isVisible&&!editView.isSelected){
            val messageToShow = message.joinToString("\n")
            Snackbar.make(
                binding.root,
                messageToShow,
                Snackbar.LENGTH_LONG)
                .apply {
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        .maxLines = 4
                }
                .show()
        }
    }

}