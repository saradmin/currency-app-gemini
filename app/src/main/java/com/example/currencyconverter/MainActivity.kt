package com.example.currencyconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

data class CurrencyItem(
    val code: String,
    val countryCode: String,
    val rateToBase: Double
)

class CurrencyViewModel : ViewModel() {
    var amountInput by mutableStateOf("100.00")
    
    var currencies = mutableStateListOf(
        CurrencyItem("USD", "us", 1.0),
        CurrencyItem("RUB", "ru", 92.50),
        CurrencyItem("EUR", "eu", 0.915),
        CurrencyItem("CNY", "cn", 7.183)
    )

    var baseIndex by mutableStateOf(0)
    var currentScreen by mutableStateOf(Screen.MAIN)
    var showAddDialog by mutableStateOf(false)

    enum class Screen { MAIN, MANAGE }

    fun setBaseCurrency(newIndex: Int) {
        if (newIndex == baseIndex || newIndex !in currencies.indices) return
        val oldRateOfNewBase = currencies[newIndex].rateToBase
        if (oldRateOfNewBase <= 0) return

        for (i in currencies.indices) {
            val updatedRate = currencies[i].rateToBase / oldRateOfNewBase
            currencies[i] = currencies[i].copy(rateToBase = updatedRate)
        }
        currencies[newIndex] = currencies[newIndex].copy(rateToBase = 1.0)
        baseIndex = newIndex
    }

    fun updateRate(index: Int, newRateStr: String) {
        val rate = newRateStr.toDoubleOrNull() ?: return
        currencies[index] = currencies[index].copy(rateToBase = rate)
    }

    fun addCurrency(code: String, countryCode: String, rate: Double) {
        if (currencies.size < 6) {
            currencies.add(CurrencyItem(code.uppercase(), countryCode.lowercase(), rate))
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CurrencyConverterApp()
                }
            }
        }
    }
}

@Composable
fun CurrencyConverterApp(vm: CurrencyViewModel = viewModel()) {
    when (vm.currentScreen) {
        CurrencyViewModel.Screen.MAIN -> MainScreen(vm)
        CurrencyViewModel.Screen.MANAGE -> ManageScreen(vm)
    }

    if (vm.showAddDialog) {
        AddCurrencyDialog(
            onDismiss = { vm.showAddDialog = false },
            onAdd = { code, countryCode, rate ->
                vm.addCurrency(code, countryCode, rate)
                vm.showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: CurrencyViewModel) {
    val baseCurrency = vm.currencies.getOrNull(vm.baseIndex) ?: return
    val baseAmount = vm.amountInput.toDoubleOrNull() ?: 0.0

    Scaffold(
        topBar = { TopAppBar(title = { Text("Конвертер валют") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = vm.amountInput,
                    onValueChange = { vm.amountInput = it },
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = { vm.currentScreen = CurrencyViewModel.Screen.MANAGE },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    FlagImage(countryCode = baseCurrency.countryCode, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(baseCurrency.code)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбор")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(vm.currencies) { index, item ->
                    if (index != vm.baseIndex) {
                        val convertedValue = baseAmount * item.rateToBase
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FlagImage(countryCode = item.countryCode, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(item.code, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = String.format("%.2f", convertedValue),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(vm: CurrencyViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выбор базы и курсы") },
                navigationIcon = {
                    IconButton(onClick = { vm.currentScreen = CurrencyViewModel.Screen.MAIN }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(vm.currencies) { index, item ->
                    val isBase = index == vm.baseIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isBase,
                            onClick = { vm.setBaseCurrency(index) }
                        )

                        FlagImage(countryCode = item.countryCode, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item.code, modifier = Modifier.width(48.dp))

                        Spacer(modifier = Modifier.width(8.dp))

                        var rateText by remember(item.rateToBase) { mutableStateOf(item.rateToBase.toString()) }

                        OutlinedTextField(
                            value = rateText,
                            onValueChange = {
                                rateText = it
                                vm.updateRate(index, it)
                            },
                            enabled = !isBase,
                            label = { Text(if (isBase) "Базовая (1.0)" else "Курс к базе") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                }
            }

            if (vm.currencies.size < 6) {
                Button(
                    onClick = { vm.showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Добавить новую валюту")
                }
            }
        }
    }
}

@Composable
fun AddCurrencyDialog(
    onDismiss: () -> Unit,
    onAdd: (code: String, countryCode: String, rate: Double) -> Unit
) {
    var currencyCode by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("1.0") }
    var previewCountryCode by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить валюту") },
        text = {
            Column {
                OutlinedTextField(
                    value = currencyCode,
                    onValueChange = { currencyCode = it },
                    label = { Text("Код валюты (напр. TRY)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { countryCode = it },
                    label = { Text("Код страны ISO (напр. tr)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { previewCountryCode = countryCode.trim().lowercase() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("🔎 Загрузить иконку")
                }

                previewCountryCode?.let { code ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Предпросмотр: ")
                        FlagImage(countryCode = code, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Начальный курс к базе") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateInput.toDoubleOrNull() ?: 1.0
                    if (currencyCode.isNotBlank() && countryCode.isNotBlank()) {
                        onAdd(currencyCode, countryCode, rate)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun FlagImage(countryCode: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val url = "https://flagcdn.com/w160/${countryCode.lowercase()}.png"

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(true)
            .build(),
        contentDescription = "Флаг $countryCode",
        modifier = modifier
    )
}
