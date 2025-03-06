package com.example.rcapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.rcapp.util.AndroidBug5497Workaround
import com.google.android.material.snackbar.Snackbar
import java.math.BigDecimal

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding :ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        enableEdgeToEdge()
        //获取状态栏的高度，并将其设置为 MaterialToolbar 的顶部内边距，让 MaterialToolbar 的内容从状态栏下方开始显示
        ViewCompat.setOnApplyWindowInsetsListener(binding.materialToolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)

        setSupportActionBar(binding.materialToolbar)
        supportActionBar?.apply {
            title = "注册"
            setDisplayHomeAsUpEnabled(true)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        binding.lifecycleOwner = this
        binding.viewModel = viewModel



        setRegisterListeners()

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun setRegisterListeners(){
        binding.registerSubmitBtn.setOnClickListener {
            val name=binding.registerNameEt.text.toString()
            val phone = binding.registerPhoneEt.text.toString()
            val password = binding.registerPasswordEt.text.toString()
            val passwordConfirm=binding.registerPasswordConfirmEt.text.toString()
            val email = binding.registerEmailBoxEt.text.toString()
            val user = User(
                name= name,
                phone = phone,
                password = password,
                email = email,
                balance = BigDecimal("0.00")
            )
            if(User.isUserValid(user)&& password == passwordConfirm){
                UserRepository(applicationContext).register(user) { success, message ->
                    if(success){
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    else{
                        Snackbar.make(binding.root,message,Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            else{
                Snackbar.make(binding.root, "请正确填写注册信息", Snackbar.LENGTH_LONG).show()
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
            val message = resources.getStringArray(R.array.register_passwordConfirm_reminder_message)
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
                .show()
        }
    }

}