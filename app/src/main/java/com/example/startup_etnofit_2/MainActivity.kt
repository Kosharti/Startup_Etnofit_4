
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
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var inputYear: NumberPicker // Замените EditText на NumberPicker
    private lateinit var spinnerMonth: Spinner
    private lateinit var inputRevenue: EditText
    private lateinit var inputChecks: EditText
    private lateinit var buttonCalculate: Button

    private lateinit var db: AppDatabase
    private lateinit var checksDataDao: ChecksDataDao
    private lateinit var buttonClearDatabase: Button

    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null

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

        // Настройка NumberPicker
        inputYear.minValue = 2010
        inputYear.maxValue = 2025
        inputYear.value = Calendar.getInstance().get(Calendar.YEAR) // Установка текущего года по умолчанию
        inputYear.wrapSelectorWheel = false // Отключаем зацикливание

        // Настройка Spinner
        val months = arrayOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
        spinnerMonth.adapter = adapter

        // Обработчик изменения года
        inputYear.setOnValueChangedListener { _, _, newVal ->
            selectedYear = newVal
            enableMonthSelection()
            loadDataIfExists()
        }

        // Обработчик выбора месяца
        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedMonth = position + 1
                loadDataIfExists()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedMonth = null
                clearInputFields()
            }
        }

        // Изначально блокируем поля ввода
        disableInputFields()

        buttonCalculate.setOnClickListener {
            calculateAndSave()
        }

    }

    private fun disableInputFields() {
        inputRevenue.isEnabled = false
        inputChecks.isEnabled = false
        buttonCalculate.isEnabled = false // Блокируем кнопку, пока не выбраны год и месяц
    }

    private fun enableInputFields() {
        inputRevenue.isEnabled = true
        inputChecks.isEnabled = true
        buttonCalculate.isEnabled = true // Разблокируем кнопку, когда выбраны год и месяц
    }

    private fun enableMonthSelection() {
        spinnerMonth.isEnabled = true
    }

    private fun loadDataIfExists() {
        if (selectedYear != null && selectedMonth != null) {
            lifecycleScope.launch {
                val existingData = withContext(Dispatchers.IO) {
                    checksDataDao.getChecksDataByYearAndMonth(selectedYear!!, selectedMonth!!)
                }

                if (existingData != null) {
                    inputRevenue.setText(existingData.revenue.toString())
                    inputChecks.setText(existingData.numberOfChecks.toString())
                } else {
                    clearInputFields()
                }
                enableInputFields() // Разблокируем поля ввода только после выбора года и месяца
            }
        }
    }

    private fun clearInputFields() {
        inputRevenue.setText("")
        inputChecks.setText("")
    }

    private fun calculateAndSave() {
        if (selectedYear == null || selectedMonth == null) {
            Toast.makeText(this, "Пожалуйста, выберите год и месяц", Toast.LENGTH_SHORT).show()
            return
        }

        val year = selectedYear!!
        val month = selectedMonth!!
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
                val year = inputYear.value
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
