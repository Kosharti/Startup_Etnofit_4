
// MainActivity.kt
package com.example.startup_etnofit_2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import java.util.*
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var inputYear: EditText
    private lateinit var spinnerMonth: Spinner
    private lateinit var inputRevenue: EditText
    private lateinit var inputChecks: EditText
    private lateinit var buttonCalculate: Button

    private lateinit var db: AppDatabase
    private lateinit var checksDataDao: ChecksDataDao
    private lateinit var buttonClearDatabase: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.checks)

        // Инициализация View
        inputYear = findViewById(R.id.inputYear)
        spinnerMonth = findViewById(R.id.spinnerMonth)
        inputRevenue = findViewById(R.id.inputRevenue)
        inputChecks = findViewById(R.id.inputChecks)
        buttonCalculate = findViewById(R.id.buttonCalculateChecks)

        // Инициализация базы данных
        db = AppDatabase.getDatabase(this)
        checksDataDao = db.checksDataDao()

        // Настройка Spinner
        val months = arrayOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
        spinnerMonth.adapter = adapter

        buttonCalculate.setOnClickListener {
            calculateAndSave()
        }

    }

    private fun calculateAndSave() {
        val yearString = inputYear.text.toString()

        if (yearString.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите год", Toast.LENGTH_SHORT).show()
            return
        }

        val year = yearString.toIntOrNull()
        if (year == null) {
            Toast.makeText(this, "Некорректный формат года", Toast.LENGTH_SHORT).show()
            return
        }

        val month = spinnerMonth.selectedItemPosition + 1 // Месяцы в Spinner 0-11, а в базе 1-12
        val revenueString = inputRevenue.text.toString()
        val numberOfChecksString = inputChecks.text.toString()

        if (revenueString.isEmpty() || numberOfChecksString.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val revenue = revenueString.toDoubleOrNull()
        val numberOfChecks = numberOfChecksString.toIntOrNull()

        if (revenue == null || numberOfChecks == null) {
            Toast.makeText(this, "Некорректный формат чисел", Toast.LENGTH_SHORT).show()
            return
        }

        val averageCheck = if (numberOfChecks > 0) revenue / numberOfChecks else 0.0

        // Логирование значений перед сохранением
        Log.d("MainActivity", "Год: $year, Месяц: $month, Выручка: $revenue, Кол-во чеков: $numberOfChecks")
        Log.d("MainActivity", "Средний чек (перед сохранением): $averageCheck")

        lifecycleScope.launch {
            // Проверяем, есть ли уже запись для данного года и месяца
            val existingData = withContext(Dispatchers.IO) {
                checksDataDao.getChecksDataByYearAndMonth(year, month)
            }

            if (existingData != null) {
                // Если запись существует, обновляем её
                val updatedData = existingData.copy(
                    revenue = revenue,
                    numberOfChecks = numberOfChecks,
                    averageCheck = averageCheck
                )
                withContext(Dispatchers.IO) {
                    checksDataDao.update(updatedData)
                }
                Log.d("MainActivity", "Данные чеков обновлены в базе данных")
            } else {
                // Если записи нет, создаем новую
                val checksData = ChecksData(
                    year = year,
                    month = month,
                    revenue = revenue,
                    numberOfChecks = numberOfChecks,
                    averageCheck = averageCheck
                )
                withContext(Dispatchers.IO) {
                    checksDataDao.insert(checksData)
                }
                Log.d("MainActivity", "Данные чеков добавлены в базу данных")
            }

            // Отображаем результаты
            showCalculationResult(averageCheck)

            // Сохраняем averageCheck в SharedPreferences
            /*val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return@launch
            with (sharedPref.edit()) {
                putFloat("average_check", averageCheck.toFloat())
                commit()
            }*/

            // Логирование среднего чека после сохранения
            /*val savedAverageCheck = sharedPref.getFloat("average_check", 0.0f).toDouble()
            Log.d("MainActivity", "Средний чек (после сохранения): $savedAverageCheck")*/
        }
    }

    private fun showCalculationResult(averageCheck: Double) {
        // Создаем AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Результат расчета")

        // Создаем layout для AlertDialog
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_result, null)

        // Находим TextView в layout
        val averageCheckTextView = view.findViewById<TextView>(R.id.averageCheckTextView)

        // Устанавливаем значение среднего чека
        averageCheckTextView.text = String.format("Средний чек: %.2f", averageCheck)

        // Устанавливаем layout для AlertDialog
        builder.setView(view)

        // Добавляем кнопку "OK"
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            // Меняем текст кнопки и переходим на следующую страницу
            buttonCalculate.text = "Далее"
            buttonCalculate.setOnClickListener {
                val intent = Intent(this, ReckoningActivity::class.java)
                val year = inputYear.text.toString().toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                val month = spinnerMonth.selectedItemPosition + 1
                intent.putExtra("year", year)
                intent.putExtra("month", month)
                startActivity(intent)
            }
        }

        // Отображаем AlertDialog
        builder.show()
    }
}
