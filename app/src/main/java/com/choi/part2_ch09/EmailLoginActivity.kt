package com.choi.part2_ch09

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.choi.part2_ch09.databinding.ActivityEmailLoginBinding

class EmailLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            doneButton.setOnClickListener {
                if (binding.emailEditText.text.isNotEmpty()) {
                    // 데이터 입력 됨
                    val data= Intent().apply {
                        putExtra("email",binding.emailEditText.text.toString())
                    }
                    setResult(RESULT_OK,data)
                    finish()
                } else {
                    Toast.makeText(this@EmailLoginActivity, "이메일을 입력해주세요", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


    }
}