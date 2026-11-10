package com.example.cryptowallet

import androidx.compose.foundation.lazy.items
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cryptowallet.ui.theme.CryptoWalletTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PriceApi::class.java)

        val database = CoinsDatabase.getInstance(applicationContext)
        val coinDao = database.getDao()
        setContent {
            CryptoWalletTheme {

                MyApp(
                    coinDao = coinDao,
                    priceApi = retrofit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val expanded = remember { mutableStateOf(false) }
    val extraPadding = if (expanded.value) 48.dp else 0.dp

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = extraPadding)
            ) {
                Text(text = "...")
                Text(text = name)
            }
            ElevatedButton(
                onClick = { expanded.value = !expanded.value }
            ) {
                Text(if (expanded.value) "Show less" else "Show more")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CryptoWalletTheme {
        Greeting("...")
    }
}

@Composable
fun MyApp(modifier: Modifier = Modifier, coinDao: CoinDao, priceApi: PriceApi) {

    var currentScreen by remember { mutableStateOf("wallet")}

    var coins by remember {mutableStateOf(emptyList<CoinHolding>())}
    var prices by remember {mutableStateOf(emptyMap<String, Double>())}

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val savedCoins = withContext(Dispatchers.IO) {
            coinDao.getAll()
        }
        coins = savedCoins

        try {
            prices = priceApi.getPrices()
        } catch (e: Exception) {

        }
    }

    Surface(modifier) {
        if (currentScreen == "wallet") {
            WalletScreen(
                coins = coins,
                prices = prices,
                onAddCoinClick = {currentScreen = "add coin"},
                onRefreshClick = {
                    scope.launch {
                        try {
                            prices = priceApi.getPrices()
                        } catch (e: Exception) {

                        }
                    }
                },
                onDeleteCoin = { coinToDelete ->
                    scope.launch(Dispatchers.IO) {
                        coinDao.delete(coinToDelete)
                        coins = coinDao.getAll()
                    }
                }
            )
        }
        else if (currentScreen == "add coin") {
            AddCoinScreen(
                onCoinAdded = {name, symbol, quantity, pricePaid ->
                    scope.launch(Dispatchers.IO) {
                        val newCoin = CoinHolding(
                            name = name,
                            symbol = symbol,
                            quantity = quantity,
                            pricePaid = pricePaid
                        )
                        coinDao.insert(newCoin)
                        coins = coinDao.getAll()
                        withContext(Dispatchers.Main) {
                            currentScreen = "wallet"
                        }
                    }
                },
                onBackClick = {
                    currentScreen = "wallet"
                }
            )
        }
    }

}

// ------------------------------------------------------------------------
@Composable
fun WalletScreen(
    coins: List<CoinHolding>,
    prices: Map<String, Double>,
    onAddCoinClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteCoin: (CoinHolding) -> Unit
) {
    val totalCost = coins.sumOf { it.quantity * it.pricePaid }
    val totalValue = coins.sumOf { it.quantity * (prices[it.symbol] ?: 0.0) }
    val totalPL = totalValue - totalCost

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Portfolio P/L")
                Text(text = "$${totalPL}")
                Button(onClick = onRefreshClick) {
                    Text("Refresh")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {

            items(coins) { coin ->
                CoinItem(coin = coin, prices = prices, onDeleteClick = { onDeleteCoin(coin) })
            }
        }

        Button(onClick = onAddCoinClick, modifier = Modifier.fillMaxWidth()) {
            Text("+ Addd Coin")
        }
    }
}


@Composable
fun CoinItem(coin: CoinHolding, prices: Map<String, Double>, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val currentPrice = prices[coin.symbol] ?: 0.0
    val currentValue = coin.quantity * currentPrice
    val coinPL = currentValue - (coin.quantity * coin.pricePaid)

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = coin.name)
                    Text(text = coin.symbol)
                }
                Text(text = "qty: ${coin.quantity}")
                Button(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "Show more")
                }
            }

            if (expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = "Current price: $${currentPrice}")
                    Text(text = "Current value: $${currentValue}")
                    Text(text = "P/L: $${coinPL}")
                    Button(onClick = onDeleteClick) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCoinScreen(
    onCoinAdded: (String, String, Double, Double) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var pricePaid by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Add Coin")

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol") })
        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
        )
        OutlinedTextField(value = pricePaid, onValueChange = { pricePaid = it }, label = { Text("Price Paid") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        Row(modifier = Modifier.padding(top = 24.dp)) {
            Button(onClick = onBackClick) { Text("Cancel") }
            Button(
                onClick = {
                    val q = quantity.toDoubleOrNull() ?: 0.0
                    val p = pricePaid.toDoubleOrNull() ?: 0.0
                    onCoinAdded(name, symbol, q, p)
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Save")
            }
        }
    }
}