package com.choi.part2_ch09

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.choi.part2_ch09.databinding.ActivityLoginBinding
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val callback : (OAuthToken?, Throwable?)->Unit = { token,error ->
        if(error!=null) {
            //로그인  찐 실패
        } else if(token!=null) {
            //로그인 성공
            Log.e("loginActivity","login with kakao account token: $token")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        KakaoSdk.init(this,BuildConfig.KAKAO)

        binding.kakaoTalkLoginButton.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    if (error != null) {
                        //로그인 실패
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }

                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    } else if(token!=null) {
                        // 로그인 성공
                        Log.e("longinActivity","token==$token")
                    }


                }
            } else {
                // 카카오 계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
            }
        }


    }
}