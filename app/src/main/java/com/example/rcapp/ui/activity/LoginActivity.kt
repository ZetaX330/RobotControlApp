package com.example.rcapp.ui.activity

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rcapp.data.repository.UserRepository
import com.example.rcapp.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import javax.inject.Inject


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var loginModel:Boolean=true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                View.ALPHA,
                1f, // 初始透明度
                1f  // 最终透明度
            )
            fadeOut.interpolator = AnticipateInterpolator()
            fadeOut.duration = 500L

            fadeOut.doOnEnd {
                splashScreenViewProvider.remove()
            }
            // 开始动画
            fadeOut.start()

        }

        // 检查用户是否已登录，并控制启动屏幕的显示时间
        splashScreen.setKeepOnScreenCondition {
            val isLoggedIn = isUserLoggedIn()
            if (isLoggedIn) {
                // 如果已登录，跳转到 MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            }
            // 返回 false 表示结束启动屏幕

            false
        }
        setLoginListeners()
    }
    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }
    private fun setLoginListeners() {

        binding.loginBtn.setOnClickListener {
            if(loginModel){
                val phone = binding.loginAccountEt.text.toString()
                val password=binding.loginPasswordEt.text.toString()
                if(phone.isNotEmpty() && password.isNotEmpty()){
                    UserRepository(this).login(phone, password) { success,message ->
                        if(success){
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this, MainActivity::class.java))
                        }
                        else{
                            Snackbar.make(binding.root,message,Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
                else{
                    Snackbar.make(binding.root,"请正确填写登录信息",Snackbar.LENGTH_LONG).show()
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