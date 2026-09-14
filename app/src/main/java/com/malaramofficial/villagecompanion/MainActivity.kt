package com.malaramofficial.villagecompanion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VillageCompanionApp() }
    }
}

private data class Category(val emoji: String, val title: String, val subtitle: String)
private data class Provider(val name: String, val village: String, val service: String, val availability: String, val phone: String? = null)

private val categories = listOf(
    Category("🌾", "कृषि मजदूर", "खेत का काम"), Category("🚜", "Tractor / मशीन", "किराये पर मशीन"),
    Category("⚡", "Electrician", "बिजली का काम"), Category("⚙️", "Motor / Pump", "मरम्मत सेवा"),
    Category("🚰", "Plumber", "पानी की लाइन"), Category("🧱", "Mason / Welding", "निर्माण काम")
)

// V1 testing selector: Barmer district -> Aadel block -> village.
// This is a sample UI catalogue; production data must come from the authoritative LGD import.
private val aadelVillages = listOf(
    "Adel", "Aadarsh Aadel", "Aadel Magji", "Bhabhuon Ka Tala", "Band", "Band Dheemji",
    "Bhanwarnagar", "Dhanasar", "Gangapura", "Goliya Jetmal", "Haroonagar", "Kalasar",
    "Khadiyali Nadi", "Khariya Khurd", "Kishanpura", "Meethi Beri", "Modaniyon Ki Dhani",
    "Nimbalkot", "Nokhra", "Pemsiddh Nagar", "Ramdevpura", "Sadecha", "Daukiyon Ki Dhani"
)

private const val PREFS = "village_companion_provider"

private fun saveProvider(context: Context, provider: Provider) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("name", provider.name).putString("village", provider.village)
        .putString("phone", provider.phone.orEmpty()).putString("service", provider.service)
        .putBoolean("available", provider.availability == "अभी उपलब्ध").apply()
}

private fun loadSavedProvider(context: Context): Provider? {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val name = p.getString("name", null)?.takeIf { it.isNotBlank() } ?: return null
    val village = p.getString("village", null)?.takeIf { it.isNotBlank() } ?: return null
    val service = p.getString("service", null)?.takeIf { it.isNotBlank() } ?: return null
    val phone = p.getString("phone", null)?.takeIf { it.isNotBlank() }
    return Provider(name, village, service, if (p.getBoolean("available", true)) "अभी उपलब्ध" else "अभी उपलब्ध नहीं", phone)
}

private fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

private fun dial(context: Context, phone: String?) {
    if (phone.isNullOrBlank()) { toast(context, "इस प्रोफाइल में मोबाइल नंबर नहीं है।"); return }
    try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))) }
    catch (_: ActivityNotFoundException) { toast(context, "फोन ऐप उपलब्ध नहीं है।") }
}

private fun whatsapp(context: Context, phone: String?, name: String) {
    if (phone.isNullOrBlank()) { toast(context, "इस प्रोफाइल में मोबाइल नंबर नहीं है।"); return }
    val d = phone.filter { it.isDigit() }
    val n = when { d.length == 10 -> "91$d"; d.length == 12 && d.startsWith("91") -> d; else -> null }
    if (n == null) { toast(context, "मोबाइल नंबर सही नहीं है।"); return }
    val msg = Uri.encode("नमस्ते $name, मुझे आपकी सेवा के बारे में जानकारी चाहिए।")
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$n?text=$msg"))) }
    catch (_: ActivityNotFoundException) { toast(context, "WhatsApp खोलने में समस्या हुई।") }
}

@Composable
fun VillageCompanionApp() {
    var screen by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<Category?>(null) }
    MaterialTheme { Surface(Modifier.fillMaxSize()) {
        when (screen) {
            "provider" -> ProviderRegistrationScreen({ screen = "home" }, { screen = "home" })
            "results" -> selected?.let { ServiceResultsScreen(it) { screen = "home" } } ?: HomeScreen({ screen = "provider" }) { selected = it; screen = "results" }
            else -> HomeScreen({ screen = "provider" }) { selected = it; screen = "results" }
        }
    } }
}

@Composable
private fun HomeScreen(onProvider: () -> Unit, onCategory: (Category) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text("🌾") }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Village Companion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("गाँव साथी • काम और सेवा") }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp)) {
                Text("गाँव में क्या चाहिए?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Barmer → Aadel के गाँव में सेवा खोजें।")
                Spacer(Modifier.height(10.dp)); Text("🙋 मुझे सेवा चाहिए", fontWeight = FontWeight.Bold)
                Text("सेवा चुनें, फिर गाँव चुनें।")
                Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onProvider, Modifier.fillMaxWidth()) { Text("👨‍🔧 मैं सेवा देता हूँ") }
            }
        }
        Text("लोकप्रिय सेवाएँ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        categories.chunked(2).forEach { rowItems -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            rowItems.forEach { c -> Card(Modifier.weight(1f).clickable { onCategory(c) }, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(c.emoji, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(6.dp)); Text(c.title, fontWeight = FontWeight.SemiBold); Text(c.subtitle, style = MaterialTheme.typography.bodySmall) } } }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        } }
        Text("सेवा चुनें → गाँव चुनें → उपलब्ध लोग देखें → सीधे संपर्क करें।")
    }
}

@Composable
private fun VillageDropdown(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().clickable { expanded = true }, label = { Text(label) }, trailingIcon = { Text("▾") })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
private fun ProviderRegistrationScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current; val existing = remember { loadSavedProvider(context) }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }; var village by remember { mutableStateOf(existing?.village ?: aadelVillages.first()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }; var service by remember { mutableStateOf(existing?.service ?: categories.first().title) }
    var available by remember { mutableStateOf(existing?.availability != "अभी उपलब्ध नहीं") }; var error by remember { mutableStateOf("") }; var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← वापस") }; Text("अपनी सेवा दर्ज करें", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("स्थान लिखने की जरूरत नहीं: Barmer → Aadel → गाँव चुनें।")
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("नाम") }, singleLine = true)
        VillageDropdown("गाँव चुनें", village, aadelVillages) { village = it }
        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), label = { Text("मोबाइल नंबर") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Text("सेवा चुनें", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        categories.forEach { c -> OutlinedButton(onClick = { service = c.title }, Modifier.fillMaxWidth()) { Text(if (service == c.title) "✓ ${c.emoji} ${c.title}" else "${c.emoji} ${c.title}") } }
        OutlinedButton(onClick = { available = !available }, Modifier.fillMaxWidth()) { Text(if (available) "🟢 अभी उपलब्ध" else "⚪ अभी उपलब्ध नहीं") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (!saved) Button(onClick = { error = when { name.isBlank() -> "नाम भरें।"; phone.length != 10 -> "10 अंकों का मोबाइल नंबर भरें।"; else -> "" }; if (error.isBlank()) { saveProvider(context, Provider(name.trim(), village, service, if (available) "अभी उपलब्ध" else "अभी उपलब्ध नहीं", phone)); saved = true } }, Modifier.fillMaxWidth()) { Text("प्रोफाइल सेव करें") }
        if (saved) { Text("✓ प्रोफाइल सेव हो गई।"); OutlinedButton(onClick = onSaved, Modifier.fillMaxWidth()) { Text("होम पर जाएँ") } }
    }
}

@Composable
private fun ServiceResultsScreen(category: Category, onBack: () -> Unit) {
    val context = LocalContext.current; val saved = remember { loadSavedProvider(context) }; var village by remember { mutableStateOf(aadelVillages.first()) }
    val demo = listOf(Provider("टेस्ट सेवा प्रदाता", aadelVillages.first(), category.title, "अभी उपलब्ध", "9999999999"), Provider("आपकी प्रोफाइल", saved?.village ?: aadelVillages.first(), saved?.service ?: category.title, saved?.availability ?: "अभी उपलब्ध", saved?.phone))
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← वापस") }; Text("${category.emoji} ${category.title}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("स्थान: Barmer → Aadel"); VillageDropdown("गाँव चुनें", village, aadelVillages) { village = it }
        val visible = demo.filter { it.service == category.title && it.village == village }
        if (visible.isEmpty()) Text("इस गाँव में अभी कोई प्रोफाइल नहीं मिली।")
        visible.forEach { p -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("📍 ${p.village}"); Text(p.availability)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { dial(context, p.phone) }, Modifier.weight(1f)) { Text("📞 Call") }; OutlinedButton(onClick = { whatsapp(context, p.phone, p.name) }, Modifier.weight(1f)) { Text("WhatsApp") } }
        } } }
        Text("Testing mode: Aadel की sample village list लगी है।")
    }
}
