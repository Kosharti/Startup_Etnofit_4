package com.example.startup_etnofit_2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class Page3Activity : AppCompatActivity() {

    private lateinit var yearTextView: TextView
    private lateinit var tableLayout: TableLayout
    private lateinit var buttonContinue: Button
    private lateinit var buttonShowResult: Button
    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var db: AppDatabase
    private lateinit var reckoningDataDao: ReckoningDataDao
    private lateinit var buttonClearDatabase: Button

    private val monthTextViews = mutableMapOf<Int, Pair<TextView, TextView>>() // Map of Month to (STextView, MTextView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_3)

        // Инициализация View
        yearTextView = findViewById(R.id.yearTextView)
        tableLayout = findViewById(R.id.tableLayout)
        buttonContinue = findViewById(R.id.buttonContinue)
        buttonShowResult = findViewById(R.id.buttonShowResult)

        // Инициализация базы данных
        db = AppDatabase.getDatabase(this)
        reckoningDataDao = db.reckoningDataDao()

        // Получаем год из Intent
        currentYear = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))
        yearTextView.text = currentYear.toString()

        // Заполняем Map Month to (STextView, MTextView)
        monthTextViews[1] = Pair(findViewById(R.id.januaryS), findViewById(R.id.januaryM))
        monthTextViews[2] = Pair(findViewById(R.id.februaryS), findViewById(R.id.februaryM))
        monthTextViews[3] = Pair(findViewById(R.id.marchS), findViewById(R.id.marchM))
        monthTextViews[4] = Pair(findViewById(R.id.aprilS), findViewById(R.id.aprilM))
        monthTextViews[5] = Pair(findViewById(R.id.mayS), findViewById(R.id.mayM))
        monthTextViews[6] = Pair(findViewById(R.id.juneS), findViewById(R.id.juneM))
        monthTextViews[7] = Pair(findViewById(R.id.julyS), findViewById(R.id.julyM))
        monthTextViews[8] = Pair(findViewById(R.id.augustS), findViewById(R.id.augustM))
        monthTextViews[9] = Pair(findViewById(R.id.septemberS), findViewById(R.id.septemberM))
        monthTextViews[10] = Pair(findViewById(R.id.octoberS), findViewById(R.id.octoberM))
        monthTextViews[11] = Pair(findViewById(R.id.novemberS), findViewById(R.id.novemberM))
        monthTextViews[12] = Pair(findViewById(R.id.decemberS), findViewById(R.id.decemberM))

        // Загрузка данных из базы данных
        loadDataForYear(currentYear)

        // Обработчик кнопки "Продолжить заполнение"
        buttonContinue.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Обработчик кнопки "Показать результат"
        buttonShowResult.setOnClickListener {
            showResult()
        }

        buttonClearDatabase = findViewById(R.id.buttonClearDatabase)
        buttonClearDatabase.setOnClickListener {
            clearDatabase()
            recreate()
        }
    }

    private fun clearDatabase() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // Нужно сначала получить экземпляр базы данных
                val db = AppDatabase.getDatabase(applicationContext)
                db.clearAllTables() // Вызываем clearAllTables() на экземпляре базы данных
            }
        }
        Toast.makeText(this, "База данных очищена", Toast.LENGTH_SHORT).show()
    }

    private fun loadDataForYear(year: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                for (month in 1..12) {
                    val data = reckoningDataDao.getReckoningDataByYearAndMonth(year, month)
                    val textViews = monthTextViews[month]
                    if (data != null && textViews != null) {
                        // Обновляем UI в главном потоке
                        withContext(Dispatchers.Main) {
                            textViews.first.text = String.format("%.2f", data.S)
                            textViews.second.text = String.format("%.2f", data.M)
                        }
                    } else if (textViews != null) {
                        // Обновляем UI в главном потоке
                        withContext(Dispatchers.Main) {
                            textViews.first.text = "-"
                            textViews.second.text = "-"
                        }
                    }
                }
            }
        }
    }


    private fun showResult() {
        lifecycleScope.launch {
            // Считаем количество месяцев с заполненными данными
            var filledMonthsCount = 0
            for (month in 1..12) {
                val data = withContext(Dispatchers.IO) {
                    reckoningDataDao.getReckoningDataByYearAndMonth(currentYear, month)
                }
                if (data != null) {
                    filledMonthsCount++
                }
            }

            if (filledMonthsCount < 2) {
                Toast.makeText(this@Page3Activity, "Слишком мало данных", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this@Page3Activity, Page4Activity::class.java)
                startActivity(intent)
            }
        }
    }

    fun onYearClicked(view: View) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            this,
            { _, yearSelected, _, _ ->
                currentYear = yearSelected
                yearTextView.text = yearSelected.toString()
                loadDataForYear(currentYear) // Загружаем данные для выбранного года
            },
            year,
            month,
            day
        )

        // Set minimum and maximum dates using Calendar
        val minDateCalendar = Calendar.getInstance()
        minDateCalendar.set(2010, 0, 1) // January 1, 2010 (month is 0-indexed)
        dpd.datePicker.minDate = minDateCalendar.timeInMillis

        val maxDateCalendar = Calendar.getInstance()
        maxDateCalendar.set(Calendar.getInstance().get(Calendar.YEAR), 11, 31) // December 31 of the current year
        dpd.datePicker.maxDate = maxDateCalendar.timeInMillis

        dpd.show()
    }
}
