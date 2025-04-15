
// ReckoningActivity.kt
package com.example.startup_etnofit_2

import android.content.Context
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
import java.util.*
import android.util.Log

class ReckoningActivity : AppCompatActivity() {

    private lateinit var spinnerRegion: Spinner
    private lateinit var inputElectricityPrev: EditText
    private lateinit var inputElectricityCurr: EditText
    private lateinit var inputGasPrev: EditText
    private lateinit var inputGasCurr: EditText
    private lateinit var inputHotWaterPrev: EditText
    private lateinit var inputHotWaterCurr: EditText
    private lateinit var inputColdWaterPrev: EditText
    private lateinit var inputColdWaterCurr: EditText
    private lateinit var buttonCalculate: Button

    private lateinit var db: AppDatabase
    private lateinit var reckoningDataDao: ReckoningDataDao
    private lateinit var checksDataDao: ChecksDataDao // Добавляем Dao для ChecksData

    private var averageCheck: Double = 0.0
    private var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var month: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reckoning)

        // Получаем данные из Intent
        year = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))
        month = intent.getIntExtra("month", 1)

        // Инициализация View
        spinnerRegion = findViewById(R.id.spinnerRegion)
        inputElectricityPrev = findViewById(R.id.inputElectricityPrev)
        inputElectricityCurr = findViewById(R.id.inputElectricityCurr)
        inputGasPrev = findViewById(R.id.inputGasPrev)
        inputGasCurr = findViewById(R.id.inputGasCurr)
        inputHotWaterPrev = findViewById(R.id.inputHotWaterPrev)
        inputHotWaterCurr = findViewById(R.id.inputHotWaterCurr)
        inputColdWaterPrev = findViewById(R.id.inputColdWaterPrev)
        inputColdWaterCurr = findViewById(R.id.inputColdWaterCurr)
        buttonCalculate = findViewById(R.id.buttonCalculate)

        // Инициализация базы данных
        db = AppDatabase.getDatabase(this)
        reckoningDataDao = db.reckoningDataDao()
        checksDataDao = db.checksDataDao() // Инициализируем ChecksDataDao

        // Настройка Spinner для выбора региона
        val regions = arrayOf("Республика Марий Эл", "Республика Татарстан", "Республика Чувашия", "Москва") // Пример регионов
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regions)
        spinnerRegion.adapter = adapter

        /*// Получаем средний чек из SharedPreferences
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        averageCheck = sharedPref.getFloat("average_check", 0.0f).toDouble()*/

        buttonCalculate.setOnClickListener {
            calculateAndSave()
        }
    }

    private fun calculateAndSave() {
        val region = spinnerRegion.selectedItem.toString()
        val electricityPrevString = inputElectricityPrev.text.toString()
        val electricityCurrString = inputElectricityCurr.text.toString()
        val gasPrevString = inputGasPrev.text.toString()
        val gasCurrString = inputGasCurr.text.toString()
        val hotWaterPrevString = inputHotWaterPrev.text.toString()
        val hotWaterCurrString = inputHotWaterCurr.text.toString()
        val coldWaterPrevString = inputColdWaterPrev.text.toString()
        val coldWaterCurrString = inputColdWaterCurr.text.toString()

        if (electricityPrevString.isEmpty() || electricityCurrString.isEmpty() ||
            gasPrevString.isEmpty() || gasCurrString.isEmpty() ||
            hotWaterPrevString.isEmpty() || hotWaterCurrString.isEmpty() ||
            coldWaterPrevString.isEmpty() || coldWaterCurrString.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val electricityPrev = electricityPrevString.toDoubleOrNull()
        val electricityCurr = electricityCurrString.toDoubleOrNull()
        val gasPrev = gasPrevString.toDoubleOrNull()
        val gasCurr = gasCurrString.toDoubleOrNull()
        val hotWaterPrev = hotWaterPrevString.toDoubleOrNull()
        val hotWaterCurr = hotWaterCurrString.toDoubleOrNull()
        val coldWaterPrev = coldWaterPrevString.toDoubleOrNull()
        val coldWaterCurr = coldWaterCurrString.toDoubleOrNull()

        if (electricityPrev == null || electricityCurr == null || gasPrev == null || gasCurr == null ||
            hotWaterPrev == null || hotWaterCurr == null || coldWaterPrev == null || coldWaterCurr == null) {
            Toast.makeText(this, "Некорректный формат чисел", Toast.LENGTH_SHORT).show()
            return
        }

        val regionCoefficient = 0.366 // Для всех регионов пока одинаковый

        lifecycleScope.launch {
            // Получаем averageCheck из базы данных
            val checksData = withContext(Dispatchers.IO) {
                checksDataDao.getChecksDataByYearAndMonth(year, month)
            }

            if (checksData != null) {
                averageCheck = checksData.averageCheck
                Log.d("ReckoningActivity", "Средний чек (из базы данных): $averageCheck")

                // Вычисления
                val E = if (averageCheck > 0) ((electricityCurr - electricityPrev) * regionCoefficient) / averageCheck else 0.0
                val G = if (averageCheck > 0) ((gasCurr - gasPrev) * 36.7 * 0.029) / averageCheck else 0.0
                val S = E + G
                val M = if (averageCheck > 0) ((hotWaterCurr - hotWaterPrev) + (coldWaterCurr - coldWaterPrev)) / averageCheck else 0.0

                Log.d("ReckoningActivity", "E: $E")
                Log.d("ReckoningActivity", "G: $G")
                Log.d("ReckoningActivity", "S: $S")
                Log.d("ReckoningActivity", "M: $M")

                // Проверяем, есть ли уже запись для данного года и месяца
                val existingData = withContext(Dispatchers.IO) {
                    reckoningDataDao.getReckoningDataByYearAndMonth(year, month)
                }

                if (existingData != null) {
                    // Если запись существует, обновляем её
                    val updatedData = existingData.copy(
                        region = region,
                        electricityPrev = electricityPrev,
                        electricityCurr = electricityCurr,
                        gasPrev = gasPrev,
                        gasCurr = gasCurr,
                        hotWaterPrev = hotWaterPrev,
                        hotWaterCurr = hotWaterCurr,
                        coldWaterPrev = coldWaterPrev,
                        coldWaterCurr = coldWaterCurr,
                        S = S,
                        M = M
                    )
                    withContext(Dispatchers.IO) {
                        reckoningDataDao.update(updatedData)
                    }
                } else {
                    // Если записи нет, создаем новую
                    val newData = ReckoningData(
                        year = year,
                        month = month,
                        region = region,
                        electricityPrev = electricityPrev,
                        electricityCurr = electricityCurr,
                        gasPrev = gasPrev,
                        gasCurr = gasCurr,
                        hotWaterPrev = hotWaterPrev,
                        hotWaterCurr = hotWaterCurr,
                        coldWaterPrev = coldWaterPrev,
                        coldWaterCurr = coldWaterCurr,
                        S = S,
                        M = M
                    )
                    withContext(Dispatchers.IO) {
                        reckoningDataDao.insert(newData)
                    }
                }

                // Показываем результаты
                showCalculationResults(E, G, S, M)
            } else {
                // Если данные о чеках не найдены
                Toast.makeText(this@ReckoningActivity, "Данные о чеках за этот период не найдены", Toast.LENGTH_SHORT).show()
                averageCheck = 1.0 // или какое-то значение по умолчанию, чтобы избежать деления на ноль
            }
        }
    }

    private fun showCalculationResults(E: Double, G: Double, S: Double, M: Double) {
        // Создаем AlertDialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Результаты расчета")

        // Создаем layout для AlertDialog
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_reckoning_result, null)

        // Находим TextView в layout
        val eTextView = view.findViewById<TextView>(R.id.eTextView)
        val gTextView = view.findViewById<TextView>(R.id.gTextView)
        val sTextView = view.findViewById<TextView>(R.id.sTextView)
        val mTextView = view.findViewById<TextView>(R.id.mTextView)

        // Устанавливаем значения
        eTextView.text = String.format("E (тонн CO2/месяц): %.2f", E)
        gTextView.text = String.format("G (тонн CO2/месяц): %.2f", G)
        sTextView.text = String.format("S: %.2f", S)
        mTextView.text = String.format("M: %.2f", M)

        // Устанавливаем layout для AlertDialog
        builder.setView(view)

        // Добавляем кнопку "OK"
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()

            // Меняем текст кнопки и переходим на следующую страницу
            buttonCalculate.text = "Далее"
            buttonCalculate.setOnClickListener {
                val intent = Intent(this, Page3Activity::class.java)
                intent.putExtra("year", year)
                startActivity(intent)
            }
        }

        // Отображаем AlertDialog
        builder.show()
    }
}
