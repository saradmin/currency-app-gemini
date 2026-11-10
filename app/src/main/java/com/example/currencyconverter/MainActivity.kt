package com.example.currencyconverter

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

data class CurrencyItem(
    val code: String,
    val countryCode: String,
    var rateToBase: Double
)

class MainActivity : AppCompatActivity() {

    private val currencies = mutableListOf(
        CurrencyItem("USD", "us", 1.0),
        CurrencyItem("RUB", "ru", 92.50),
        CurrencyItem("EUR", "eu", 0.915),
        CurrencyItem("CNY", "cn", 7.183)
    )
    private var baseIndex = 0
    private var currentAmount = 100.0

    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var rootContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
        }
        setContentView(rootContainer)
        showMainScreen()
    }

    private fun showMainScreen() {
        rootContainer.removeAllViews()

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Конвертер валют"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        mainLayout.addView(title)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val amountInput = EditText(this).apply {
            hint = "Сумма"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.2f", currentAmount))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputRow.addView(amountInput)

        val baseCurrency = currencies[baseIndex]
        val flagBtn = Button(this).apply {
            text = "${baseCurrency.code} ▼"
            setOnClickListener { showManageScreen() }
        }
        inputRow.addView(flagBtn)
        mainLayout.addView(inputRow)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 0)
        }

        fun updateCalculations() {
            listContainer.removeAllViews()
            for ((idx, item) in currencies.withIndex()) {
                if (idx == baseIndex) continue
                val converted = currentAmount * item.rateToBase

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 16, 0, 16)
                }

                val flagImg = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(80, 56)
                }
                loadFlag(item.countryCode, flagImg)
                row.addView(flagImg)

                val codeTxt = TextView(this).apply {
                    text = "  ${item.code}"
                    textSize = 18f
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(codeTxt)

                val valTxt = TextView(this).apply {
                    text = String.format(Locale.US, "%.2f", converted)
                    textSize = 20f
                }
                row.addView(valTxt)

                listContainer.addView(row)
            }
        }

        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentAmount = s.toString().toDoubleOrNull() ?: 0.0
                updateCalculations()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        updateCalculations()
        scrollView.addView(listContainer)
        mainLayout.addView(scrollView)
        rootContainer.addView(mainLayout)
    }

    private fun showManageScreen() {
        rootContainer.removeAllViews()

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val backBtn = Button(this).apply {
            text = "← Назад"
            setOnClickListener { showMainScreen() }
        }
        val title = TextView(this).apply {
            text = "  Курсы валют"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }
        headerRow.addView(backBtn)
        headerRow.addView(title)
        mainLayout.addView(headerRow)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 0)
        }

        for ((idx, item) in currencies.withIndex()) {
            val isBase = idx == baseIndex
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 12)
            }

            val radio = RadioButton(this).apply {
                isChecked = isBase
                setOnClickListener {
                    setBaseCurrency(idx)
                    showManageScreen()
                }
            }
            row.addView(radio)

            val flagImg = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(60, 42)
            }
            loadFlag(item.countryCode, flagImg)
            row.addView(flagImg)

            val codeTxt = TextView(this).apply {
                text = " ${item.code} "
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
            }
            row.addView(codeTxt)

            val rateEdit = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(item.rateToBase.toString())
                isEnabled = !isBase
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val r = s.toString().toDoubleOrNull() ?: return
                        currencies[idx].rateToBase = r
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
            }
            row.addView(rateEdit)
            listContainer.addView(row)
        }

        scrollView.addView(listContainer)
        mainLayout.addView(scrollView)

        if (currencies.size < 6) {
            val addBtn = Button(this).apply {
                text = "+ Добавить новую валюту"
                setOnClickListener { showAddDialog() }
            }
            mainLayout.addView(addBtn)
        }

        rootContainer.addView(mainLayout)
    }

    private fun setBaseCurrency(newIndex: Int) {
        if (newIndex == baseIndex) return
        val oldRate = currencies[newIndex].rateToBase
        if (oldRate <= 0) return
        for (i in currencies.indices) {
            currencies[i].rateToBase /= oldRate
        }
        currencies[newIndex].rateToBase = 1.0
        baseIndex = newIndex
    }

    private fun showAddDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Добавить валюту")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val codeInput = EditText(this).apply { hint = "Код (напр. TRY)" }
        val countryInput = EditText(this).apply { hint = "Страна ISO (напр. tr)" }
        val rateInput = EditText(this).apply {
            hint = "Начальный курс к базе"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("1.0")
        }

        val previewRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val previewFlag = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(80, 56)
        }
        val loadBtn = Button(this).apply {
            text = "🔎 Иконка"
            setOnClickListener {
                val cCode = countryInput.text.toString().trim().lowercase()
                if (cCode.isNotEmpty()) loadFlag(cCode, previewFlag)
            }
        }
        previewRow.addView(loadBtn)
        previewRow.addView(previewFlag)

        layout.addView(codeInput)
        layout.addView(countryInput)
        layout.addView(previewRow)
        layout.addView(rateInput)

        builder.setView(layout)
        builder.setPositiveButton("Сохранить") { dialog, _ ->
            val code = codeInput.text.toString().trim().uppercase()
            val country = countryInput.text.toString().trim().lowercase()
            val rate = rateInput.text.toString().toDoubleOrNull() ?: 1.0
            if (code.isNotEmpty() && country.isNotEmpty() && currencies.size < 6) {
                currencies.add(CurrencyItem(code, country, rate))
                showManageScreen()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun loadFlag(countryCode: String, imageView: ImageView) {
        executor.execute {
            try {
                val url = URL("https://flagcdn.com/w160/${countryCode.lowercase()}.png")
                val bitmap = BitmapFactory.decodeStream(url.openStream())
                mainHandler.post { imageView.setImageBitmap(bitmap) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
