package com.shabakat.asdekaa

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class CardType(
    val name: String,
    var initial: Int,
    var added: Int,
    var sold: Int,
    var price: Double
) {
    val remaining: Int
        get() = initial + added - sold
}

data class Invoice(
    val date: String,
    val card: String,
    val quantity: Int,
    val price: Double,
    val total: Double
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                // ضبط اتجاه التطبيق ليكون من اليمين إلى اليسار للغة العربية
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    App()
                }
            }
        }
    }
}

class AppStorage(private val context: Context) {

    private val prefs = context.getSharedPreferences("shabakat_data", Context.MODE_PRIVATE)

    fun loadCards(): MutableList<CardType> {
        val text = prefs.getString("cards", null)
        if (text == null) {
            return mutableListOf(
                CardType("4 جنيه", 0, 0, 0, 4.0),
                CardType("8 جنيه", 0, 0, 0, 8.0),
                CardType("23 جنيه", 0, 0, 0, 23.0),
                CardType("46 جنيه", 0, 0, 0, 46.0),
                CardType("96 جنيه", 0, 0, 0, 96.0)
            )
        }

        val result = mutableListOf<CardType>()
        val array = JSONArray(text)
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                CardType(
                    o.getString("name"),
                    o.getInt("initial"),
                    o.getInt("added"),
                    o.getInt("sold"),
                    o.getDouble("price")
                )
            )
        }
        return result
    }

    fun saveCards(cards: List<CardType>) {
        val array = JSONArray()
        cards.forEach {
            val o = JSONObject()
            o.put("name", it.name)
            o.put("initial", it.initial)
            o.put("added", it.added)
            o.put("sold", it.sold)
            o.put("price", it.price)
            array.put(o)
        }
        prefs.edit().putString("cards", array.toString()).apply()
    }

    fun loadInvoices(): MutableList<Invoice> {
        val text = prefs.getString("invoices", null) ?: return mutableListOf()
        val result = mutableListOf<Invoice>()
        val array = JSONArray(text)
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                Invoice(
                    o.getString("date"),
                    o.getString("card"),
                    o.getInt("quantity"),
                    o.getDouble("price"),
                    o.getDouble("total")
                )
            )
        }
        return result
    }

    fun saveInvoices(invoices: List<Invoice>) {
        val array = JSONArray()
        invoices.forEach {
            val o = JSONObject()
            o.put("date", it.date)
            o.put("card", it.card)
            o.put("quantity", it.quantity)
            o.put("price", it.price)
            o.put("total", it.total)
            array.put(o)
        }
        prefs.edit().putString("invoices", array.toString()).apply()
    }
}

@Composable
fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storage = remember { AppStorage(context) }

    // استخدام mutableStateListOf يضمن تحديث الواجهة تلقائياً وبسرعة عند أي تغيير
    val cards = remember { mutableStateListOf<CardType>().apply { addAll(storage.loadCards()) } }
    val invoices = remember { mutableStateListOf<Invoice>().apply { addAll(storage.loadInvoices()) } }

    var selectedPage by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedPage == 0,
                    onClick = { selectedPage = 0 },
                    icon = {},
                    label = { Text("فاتورة جديدة") }
                )
                NavigationBarItem(
                    selected = selectedPage == 1,
                    onClick = { selectedPage = 1 },
                    icon = {},
                    label = { Text("الفواتير") }
                )
                NavigationBarItem(
                    selected = selectedPage == 2,
                    onClick = { selectedPage = 2 },
                    icon = {},
                    label = { Text("المخزون") }
                )
                NavigationBarItem(
                    selected = selectedPage == 3,
                    onClick = { selectedPage = 3 },
                    icon = {},
                    label = { Text("التقارير") }
                )
                NavigationBarItem(
                    selected = selectedPage == 4,
                    onClick = { selectedPage = 4 },
                    icon = {},
                    label = { Text("الأسعار") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedPage) {
                0 -> NewInvoiceScreen(
                    cards = cards,
                    onSave = { cardIndex, quantity ->
                        val card = cards[cardIndex]
                        if (quantity <= card.remaining) {
                            val price = card.price
                            val total = quantity * price

                            cards[cardIndex] = card.copy(sold = card.sold + quantity)

                            val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
                            invoices.add(0, Invoice(date, card.name, quantity, price, total))

                            storage.saveCards(cards)
                            storage.saveInvoices(invoices)
                        }
                    }
                )

                1 -> InvoiceListScreen(invoices)

                2 -> InventoryScreen(
                    cards = cards,
                    onAdd = { index, quantity ->
                        if (quantity > 0) {
                            cards[index] = cards[index].copy(added = cards[index].added + quantity)
                            storage.saveCards(cards)
                        }
                    }
                )

                3 -> ReportsScreen(cards = cards, invoices = invoices)

                4 -> PricesScreen(
                    cards = cards,
                    onPriceChange = { index, price ->
                        if (price >= 0) {
                            cards[index] = cards[index].copy(price = price)
                            storage.saveCards(cards)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NewInvoiceScreen(
    cards: List<CardType>,
    onSave: (Int, Int) -> Unit
) {
    var selected by remember { mutableStateOf(0) }
    var quantityText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val quantity = quantityText.toIntOrNull() ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "فاتورة جديدة",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(20.dp))
        Text("نوع الكارت")
        Spacer(Modifier.height(8.dp))

        cards.forEachIndexed { index, card ->
            Row(modifier = Modifier.fillMaxWidth()) {
                RadioButton(
                    selected = selected == index,
                    onClick = {
                        selected = index
                        message = ""
                    }
                )
                Column {
                    Text(card.name)
                    Text("المتبقي: ${card.remaining} - السعر: ${card.price} جنيه")
                }
            }
        }

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
            label = { Text("الكمية") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = {
                val remaining = cards.getOrNull(selected)?.remaining ?: 0
                when {
                    quantity <= 0 -> message = "اكتب كمية صحيحة"
                    quantity > remaining -> message = "الكمية أكبر من المخزون المتبقي"
                    else -> {
                        onSave(selected, quantity)
                        quantityText = ""
                        message = "تم حفظ الفاتورة وخصم الكمية من المخزون"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ الفاتورة")
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(15.dp))
            Text(message)
        }
    }
}

@Composable
fun InvoiceListScreen(invoices: List<Invoice>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "الفواتير",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(15.dp))

        if (invoices.isEmpty()) {
            Text("لا توجد فواتير حتى الآن")
        } else {
            LazyColumn {
                items(invoices) { invoice ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(15.dp)) {
                            Text("التاريخ: ${invoice.date}")
                            Text("الكارت: ${invoice.card}")
                            Text("الكمية: ${invoice.quantity}")
                            Text("سعر الوحدة: ${invoice.price} جنيه")
                            Text("الإجمالي: ${invoice.total} جنيه")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryScreen(
    cards: List<CardType>,
    onAdd: (Int, Int) -> Unit
) {
    var addIndex by remember { mutableStateOf(-1) }
    var quantityText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "المخزون",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(15.dp))

        LazyColumn {
            items(cards.indices.toList()) { index ->
                val card = cards[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            card.name,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text("الرصيد الأولي: ${card.initial}")
                        Text("المضاف: ${card.added}")
                        Text("المباع: ${card.sold}")
                        Text("المتبقي: ${card.remaining}")

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                addIndex = index
                                quantityText = ""
                            }
                        ) {
                            Text("إضافة كمية")
                        }

                        if (addIndex == index) {
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                                label = { Text("الكمية المضافة") }
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val q = quantityText.toIntOrNull() ?: 0
                                    if (q > 0) {
                                        onAdd(index, q)
                                        addIndex = -1
                                        quantityText = ""
                                    }
                                }
                            ) {
                                Text("حفظ")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(
    cards: List<CardType>,
    invoices: List<Invoice>
) {
    val totalSales = invoices.sumOf { it.total }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "التقارير",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "إجمالي المبيعات: $totalSales جنيه",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(15.dp))

        LazyColumn {
            items(cards) { card ->
                val sales = invoices.filter { it.card == card.name }.sumOf { it.total }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            card.name,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text("الكمية الأولية: ${card.initial}")
                        Text("الكمية المضافة: ${card.added}")
                        Text("الكمية المباعة: ${card.sold}")
                        Text("المتبقي: ${card.remaining}")
                        Text("قيمة المبيعات: $sales جنيه")
                    }
                }
            }
        }
    }
}

@Composable
fun PricesScreen(
    cards: List<CardType>,
    onPriceChange: (Int, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "الأسعار",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(15.dp))

        LazyColumn {
            items(cards.indices.toList()) { index ->
                var priceText by remember(cards[index].price) {
                    mutableStateOf(cards[index].price.toString())
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            cards[index].name,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("السعر الجديد") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val price = priceText.toDoubleOrNull()
                                if (price != null && price >= 0) {
                                    onPriceChange(index, price)
                                }
                            }
                        ) {
                            Text("حفظ السعر")
                        }
                    }
                }
            }
        }
    }
}
