package com.malaramofficial.villagecompanion

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VillageCompanionApp() }
    }
}

private data class Category(val emoji: String, val title: String, val subtitle: String)
private data class Provider(
    val name: String,
    val village: String,
    val service: String,
    val availability: String,
    val rating: String,
    val phone: String? = null
)

private val categories = listOf(
    Category("🌾", "कृषि मजदूर", "खेत का काम"),
    Category("🚜", "Tractor / मशीन", "किराये पर मशीन"),
    Category("⚡", "Electrician", "बिजली का काम"),
    Category("⚙️", "Motor / Pump", "मरम्मत सेवा"),
    Category("🚰", "Plumber", "पानी की लाइन"),
    Category("🧱", "Mason / Welding", "निर्माण काम")
)

private val demoProviders = listOf(
    Provider("रामलाल", "मीठी बेरी", "कृषि मजदूर", "आज उपलब्ध", "4.8 ★"),
    Provider("हनुमान राम", "नोकड़ा", "Tractor / मशीन", "अभी उपलब्ध", "4.7 ★"),
    Provider("मोहनलाल", "डऊकीयो की ढाणी", "Electrician", "आज उपलब्ध", "4.9 ★")
)

private const val PREFS = "village_companion_provider"

private fun saveProvider(context: Context, provider: Provider) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("name", provider.name)
        .putString("village", provider.village)
        .putString("phone", provider.phone.orEmpty())
        .putString("service", provider.service)
        .putBoolean("available", provider.availability == "अभी उपलब्ध")
        .apply()
}

private fun loadSavedProvider(context: Context): Provider? {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val name = prefs.getString("name", null)?.takeIf { it.isNotBlank() } ?: return null
    val village = prefs.getString("village", null)?.takeIf { it.isNotBlank() } ?: return null
    val service = prefs.getString("service", null)?.takeIf { it.isNotBlank() } ?: return null
    val phone = prefs.getString("phone", null)?.takeIf { it.isNotBlank() }
    val available = prefs.getBoolean("available", true)
    return Provider(name, village, service, if (available) "अभी उपलब्ध" else "अभी उपलब्ध नहीं", "नई ★", phone)
}

private fun dialProvider(context: Context, phone: String?) {
    if (phone.isNullOrBlank()) return
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
}

private fun whatsappProvider(context: Context, phone: String?, providerName: String) {
    if (phone.isNullOrBlank()) return
    val normalized = phone.filter { it.isDigit() }.let { if (it.length == 10) "91$it" else it }
    val message = Uri.encode("नमस्ते $providerName, मुझे आपकी सेवा के बारे में जानकारी चाहिए।")
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized?text=$message")))
}

@Composable
fun VillageCompanionApp() {
    var screen by remember { mutableStateOf("home") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (screen) {
                "provider" -> ProviderRegistrationScreen(
                    onBack = { screen = "home" },
                    onSaved = { screen = "home" }
                )
                "results" -> ServiceResultsScreen(
                    category = selectedCategory!!,
                    onBack = { screen = "home" }
                )
                else -> HomeScreen(
                    onProvider = { screen = "provider" },
                    onCategory = {
                        selectedCategory = it
                        screen = "results"
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onProvider: () -> Unit, onCategory: (Category) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Text("🌾", style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Village Companion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("गाँव साथी • काम और सेवा")
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("गाँव में क्या चाहिए?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("अपने आसपास उपलब्ध व्यक्ति या मशीन खोजें।")
                Spacer(Modifier.height(12.dp))
                Text("🙋 मुझे सेवा चाहिए", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("नीचे कोई सेवा चुनें।")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onProvider, Modifier.fillMaxWidth()) {
                    Text("👨‍🔧 मैं सेवा देता हूँ")
                }
            }
        }

        Text("लोकप्रिय सेवाएँ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(390.dp),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                Card(
                    Modifier.fillMaxWidth().clickable { onCategory(category) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(category.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(category.title, fontWeight = FontWeight.SemiBold)
                        Text(category.subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Text("सेवा चुनें → उपलब्ध लोग देखें → सीधे संपर्क करें।")
    }
}

@Composable
private fun ProviderRegistrationScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val existing = remember { loadSavedProvider(context) }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var village by remember { mutableStateOf(existing?.village.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var selectedService by remember { mutableStateOf(categories.firstOrNull { it.title == existing?.service }) }
    var available by remember { mutableStateOf(existing?.availability != "अभी उपलब्ध नहीं") }
    var error by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("← वापस") }
        Text("अपनी सेवा दर्ज करें", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("प्रोफाइल अभी इसी फोन में सुरक्षित रहेगी। Firebase बाद में जोड़ा जाएगा।")

        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("नाम") }, singleLine = true)
        OutlinedTextField(village, { village = it }, Modifier.fillMaxWidth(), label = { Text("गाँव") }, singleLine = true)
        OutlinedTextField(
            phone,
            { phone = it.filter { ch -> ch.isDigit() }.take(10) },
            Modifier.fillMaxWidth(),
            label = { Text("मोबाइल नंबर") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Text("सेवा चुनें", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        categories.forEach { category ->
            OutlinedButton(onClick = { selectedService = category }, Modifier.fillMaxWidth()) {
                Text(if (selectedService == category) "✓ ${category.emoji} ${category.title}" else "${category.emoji} ${category.title}")
            }
        }

        OutlinedButton(onClick = { available = !available }, Modifier.fillMaxWidth()) {
            Text(if (available) "🟢 अभी उपलब्ध" else "⚪ अभी उपलब्ध नहीं")
        }

        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)

        if (saved) {
            Text("✓ प्रोफाइल इसी फोन में सेव हो गई है।")
        } else {
            Button(
                onClick = {
                    error = when {
                        name.isBlank() -> "नाम भरें।"
                        village.isBlank() -> "गाँव का नाम भरें।"
                        phone.length != 10 -> "10 अंकों का मोबाइल नंबर भरें।"
                        selectedService == null -> "एक सेवा चुनें।"
                        else -> ""
                    }
                    if (error.isBlank()) {
                        saveProvider(
                            context,
                            Provider(name.trim(), village.trim(), selectedService!!.title, if (available) "अभी उपलब्ध" else "अभी उपलब्ध नहीं", "नई ★", phone)
                        )
                        saved = true
                    }
                },
                Modifier.fillMaxWidth()
            ) { Text("प्रोफाइल सेव करें") }
        }
        if (saved) {
            OutlinedButton(onClick = onSaved, Modifier.fillMaxWidth()) { Text("होम पर जाएँ") }
        }
    }
}

@Composable
private fun ServiceResultsScreen(category: Category, onBack: () -> Unit) {
    val context = LocalContext.current
    val savedProvider = remember { loadSavedProvider(context) }
    var village by remember { mutableStateOf("मेरा गाँव") }
    val villages = listOf("मेरा गाँव", "नोकड़ा", "मीठी बेरी", "डऊकीयो की ढाणी")
    val allProviders = buildList {
        addAll(demoProviders)
        if (savedProvider != null) add(savedProvider)
    }
    val filtered = allProviders.filter { it.service == category.title && (village == "मेरा गाँव" || it.village == village) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("← वापस") }
        Text("${category.emoji} ${category.title}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("अपने आसपास सेवा देने वाले लोग खोजें।")
        Text("गाँव चुनें", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        villages.forEach { item ->
            OutlinedButton(onClick = { village = item }, Modifier.fillMaxWidth()) {
                Text(if (village == item) "✓ $item" else item)
            }
        }

        Text("${filtered.size} उपलब्ध प्रोफाइल", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (filtered.isEmpty()) {
            Text("इस सेवा और गाँव के लिए अभी कोई प्रोफाइल नहीं मिली।")
        } else {
            filtered.forEach { ProviderCard(it) }
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider) {
    val context = LocalContext.current
    val hasPhone = !provider.phone.isNullOrBlank()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, Modifier.weight(1f))
                Text(provider.rating)
            }
            Text("${provider.service} • ${provider.village}")
            Text(if (provider.availability == "अभी उपलब्ध") "🟢 ${provider.availability}" else "⚪ ${provider.availability}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = hasPhone, onClick = { dialProvider(context, provider.phone) }) { Text("📞 Call") }
                OutlinedButton(enabled = hasPhone, onClick = { whatsappProvider(context, provider.phone, provider.name) }) { Text("💬 WhatsApp") }
            }
            if (!hasPhone) Text("Demo प्रोफाइल — वास्तविक संपर्क नंबर अभी उपलब्ध नहीं है।", style = MaterialTheme.typography.bodySmall)
        }
    }
}
