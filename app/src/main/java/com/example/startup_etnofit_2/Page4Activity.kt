package com.example.startup_etnofit_2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button


class Page4Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_4)

        val backButton: Button = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            // Действие при нажатии на кнопку "Вернуться в начало"
            val intent = Intent(this, MainActivity::class.java) // Замени MainActivity на название твоего главного Activity
            startActivity(intent)
            finish() // Закрываем текущую Activity, чтобы пользователь не мог вернуться назад кнопкой "Назад"
        }
    }
}