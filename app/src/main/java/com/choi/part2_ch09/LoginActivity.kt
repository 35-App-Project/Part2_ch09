package com.choi.part2_ch09

import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.P
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Email
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.choi.part2_ch09.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var pendingUser: User
    private lateinit var emailLoginResult: ActivityResultLauncher<Intent>

    private val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            //로그인  찐 실패
            showErrorToast()
            error.printStackTrace()
        } else if (token != null) {
            //로그인 성공
            getKakaoAccountInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        emailLoginResult=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode== RESULT_OK) {
                val email=it.data?.getStringExtra("email")

                if (email==null) {
                    showErrorToast()
                    return@registerForActivityResult
                } else {
                    signInFirebase(pendingUser,email)
                }
            }
        }
        KakaoSdk.init(this, BuildConfig.KAKAO)

        if (AuthApiClient.instance.hasToken()) {
            UserApiClient.instance.accessTokenInfo {tokenInfo, error ->
                if (error==null) {
                    getKakaoAccountInfo()
                }
            }
        }

        binding.kakaoTalkLoginButton.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    if (error != null) {
                        //로그인 실패
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }

                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    } else if (token != null) {
                        // 로그인 성공 (Firebase에도 로그인 되어있는지 확인)
                        if (Firebase.auth.currentUser == null) {
                            getKakaoAccountInfo()
                        } else {
                            navigateToMapActivity()
                        }
                    }


                }
            } else {
                // 카카오 계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
            }
        }
    }

    private fun getKakaoAccountInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                showErrorToast()
                error.printStackTrace()
            } else if (user != null) {
                // 사용자 정보 요청 성공
                Log.e("LoginActivity", "user 회원번호 : ${user.id} / 이메일 : ${user.kakaoAccount?.email}")
                checkKakaoUserData(user)
            }
        }
    }

    private fun checkKakaoUserData(user: User) {
        val kakaoEmail = user.kakaoAccount?.email.orEmpty()

        if (kakaoEmail.isEmpty()) {
            // 추가로 이메일을 받는 작업
            pendingUser=user
            emailLoginResult.launch(Intent(this,EmailLoginActivity::class.java))
            return
        }

        signInFirebase(user, kakaoEmail)
    }

    private fun navigateToMapActivity() {
        startActivity(Intent(this, MapActivity::class.java))
    }

    private fun showErrorToast() {
        Toast.makeText(this, "사용자 로그인에 실패했습니다", Toast.LENGTH_SHORT).show()
    }

    private fun signInFirebase(user: User, kakaoEmail: String) {
        val uid = user.id.toString()
        Firebase.auth.createUserWithEmailAndPassword(
            kakaoEmail, uid
        ).addOnCompleteListener {
            if (it.isSuccessful) {
                // 로그인 후 다음과정
                updateFirebaseDatabase(user)
            }
        }.addOnFailureListener {
            // 이미 가입된 계정
            if (it is FirebaseAuthUserCollisionException) {
                Firebase.auth.signInWithEmailAndPassword(kakaoEmail, uid)
                    .addOnCompleteListener { result ->
                        if (result.isSuccessful) {
                            // 로그인 후 다음과정
                            updateFirebaseDatabase(user)
                        } else {
                            showErrorToast()
                        }
                    }.addOnFailureListener { error ->
                    error.printStackTrace()
                    showErrorToast()
                }
            } else {
                showErrorToast()
            }
        }
    }
    // 가져온 정보 업데이트
    private fun updateFirebaseDatabase(user: User) {
        val uid=Firebase.auth.currentUser?.uid.toString()

        val personMap= mutableMapOf<String, Any>()
        personMap["uid"]=uid
        personMap["name"]=user.kakaoAccount?.profile?.nickname.orEmpty()
        personMap["profilePhoto"]=user.kakaoAccount?.profile?.thumbnailImageUrl.orEmpty()

        Firebase.database.reference.child("Person").child(uid).updateChildren(personMap)

        navigateToMapActivity()
    }
}