package com.malaramofficial.villagecompanion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VillageCompanionApp() }
    }
}

private data class Category(val id: String? = null, val emoji: String, val title: String, val subtitle: String)
private data class Provider(val name: String, val village: String, val service: String, val availability: String, val phone: String? = null)

private val fallbackCategories = listOf(
    Category(emoji = "🌾", title = "कृषि मजदूर", subtitle = "खेत का काम"), Category(emoji = "🚜", title = "Tractor / मशीन", subtitle = "किराये पर मशीन"),
    Category(emoji = "⚡", title = "Electrician", subtitle = "बिजली का काम"), Category(emoji = "⚙️", title = "Motor / Pump", subtitle = "मरम्मत सेवा"),
    Category(emoji = "🚰", title = "Plumber", subtitle = "पानी की लाइन"), Category(emoji = "🧱", title = "Mason / Welding", subtitle = "निर्माण काम")
)

private suspend fun loadCategories(): List<Category> = runCatching {
    SupabaseRepository.getActiveServices().map { service ->
        Category(
            id = service.id,
            emoji = service.emoji ?: "📌",
            title = service.name,
            subtitle = service.subtitle ?: "सेवा"
        )
    }
}.getOrElse { fallbackCategories }

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
    var categories by remember { mutableStateOf<List<Category>>(fallbackCategories) }

    LaunchedEffect(Unit) {
        categories = loadCategories()
    }

    MaterialTheme { Surface(Modifier.fillMaxSize()) {
        when (screen) {
            "provider" -> ProviderRegistrationScreen(categories = categories, onBack = { screen = "home" }, onSaved = { screen = "home" })
            "results" -> selected?.let { ServiceResultsScreen(category = it, categories = categories, onBack = { screen = "home" }) } ?: HomeScreen(categories = categories, onProvider = { screen = "provider" }) { selected = it; screen = "results" }
            else -> HomeScreen(categories = categories, onProvider = { screen = "provider" }) { selected = it; screen = "results" }
        }
    } }
}

@Composable
private fun HomeScreen(categories: List<Category>, onProvider: () -> Unit, onCategory: (Category) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text("🌾") }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Village Companion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("गाँव में सेवा ढूँढें") }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp)) {
                Text("गाँव में क्या चाहिए?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Barmer जिले के गाँव में सेवा खोजें।")
                Spacer(Modifier.height(10.dp)); Text("���� मुझे सेवा चाहिए", fontWeight = FontWeight.Bold)
                Text("सेवा चुनें → आपकी लोकेशन अपने-आप पता होगी → उपलब्ध लोग देखें")
                Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onProvider, Modifier.fillMaxWidth()) { Text("👨‍🔧 मैं सेवा देता हूँ") }
            }
        }

        Text("लोकप्रिय सेवाएँ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        categories.chunked(2).forEach { rowItems -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            rowItems.forEach { c -> Card(Modifier.weight(1f).clickable { onCategory(c) }, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(c.emoji, style = MaterialTheme.typography.headlineSmall); Text(c.title, fontWeight = FontWeight.Bold); if (c.subtitle.isNotBlank()) Text(c.subtitle) } } }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        } }
        Text("सेवा चुनें → लोकेशन अपने-आप पता होगी → उपलब्ध लोग देखें → सीधे संपर्क करें।")
    }
}

@Composable
private fun LocationDropdown(label: String, value: String, options: List<String>, enabled: Boolean = true, loading: Boolean = false, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { expanded = true },
            label = { Text(label) },
            trailingIcon = { if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("▾") }
        )
        if (enabled && options.isNotEmpty()) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
            }
        }
    }
}

private fun normalizeLocationText(value: String?): String = value.orEmpty()
    .lowercase()
    .replace("़", "")
    .replace("\\u200c", "")
    .replace("\\u200d", "")
    .replace(" ", "")
    .replace("-", "")
    .replace("_", "")
    .replace(",", "")
    .replace(".", "")

private fun locationTextCandidates(address: Address): List<String> = listOfNotNull(
    address.featureName,
    address.subLocality,
    address.locality,
    address.subAdminArea,
    address.adminArea
).flatMap { listOf(it, it.removeSuffix(" Village"), it.removeSuffix(" village")) }

private fun matchVillage(address: Address, villages: List<VillageRow>): VillageRow? {
    val candidates = locationTextCandidates(address).map(::normalizeLocationText).filter { it.isNotBlank() }
    return villages.firstOrNull { village ->
        val name = normalizeLocationText(village.name)
        name.isNotBlank() && candidates.any { candidate -> candidate == name || candidate.contains(name) || name.contains(candidate) }
    }
}

private suspend fun currentLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
    if (provider == null) { cont.resume(null); return@suspendCancellableCoroutine }
    val last = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    if (last != null && System.currentTimeMillis() - last.time < 120_000L) {
        cont.resume(last); return@suspendCancellableCoroutine
    }
    val signal = CancellationSignal()
    cont.invokeOnCancellation { signal.cancel() }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                if (cont.isActive) cont.resume(location)
            }
        }.onFailure { if (cont.isActive) cont.resume(null) }
    } else {
        if (cont.isActive) cont.resume(last)
    }
}

private suspend fun reverseGeocode(context: Context, location: Location): Address? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    runCatching {
        Geocoder(context).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
    }.getOrNull()
}

@Composable
private fun AutoLocationSelector(onVillageSelected: (VillageRow) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<VillageRow?>(null) }
    var status by remember { mutableStateOf("आपकी लोकेशन खोजी जा रही है…") }
    var addressText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var villages by remember { mutableStateOf<List<VillageRow>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) scope.launch { detect() } else status = "लोकेशन अनुमति चाहिए।"
    }

    suspend fun detect() {
        loading = true
        status = "आपकी लोकेशन खोजी जा रही है…"
        addressText = ""
        val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            loading = false
            permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        val allVillages = runCatching { SupabaseRepository.getActiveVillagesForAutoDetect() }.getOrElse {
            loading = false
            status = "गाँवों की सूची नहीं मिल सकी। इंटरनेट जाँचें।"
            return
        }
        villages = allVillages
        val location = currentLocation(context)
        if (location == null) {
            loading = false
            status = "GPS लोकेशन नहीं मिली। GPS चालू करके फिर कोशिश करें।"
            return
        }
        val address = reverseGeocode(context, location)
        addressText = address?.getAddressLine(0).orEmpty()
        val match = address?.let { matchVillage(it, allVillages) }
        if (match != null) {
            selected = match
            onVillageSelected(match)
            status = "✓ गाँव अपने-आप चुन लिया गया"
        } else {
            status = "आपकी लोकेशन मिली, लेकिन गाँव का नाम database से match नहीं हुआ।"
        }
        loading = false
    }

    LaunchedEffect(Unit) { detect() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("📍 आपका गाँव", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Text(selected?.name ?: status, fontWeight = FontWeight.SemiBold)
                if (addressText.isNotBlank()) Text(addressText, style = MaterialTheme.typography.bodySmall)
                if (selected != null) Text("✓ स्थान अपने-आप चुना गया", color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { scope.launch { detect() } }, enabled = !loading, Modifier.fillMaxWidth()) {
                    Text("📍 मेरी लोकेशन फिर से खोजें")
                }
            }
        }
    }
}

@Composable
private fun ProviderRegistrationScreen(categories: List<Category>, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current; val existing = remember { loadSavedProvider(context) }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var village by remember { mutableStateOf(existing?.village.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var service by remember { mutableStateOf(existing?.service ?: categories.firstOrNull()?.title ?: "कृषि मजदूर") }
    var available by remember { mutableStateOf(existing?.availability != "अभी उपलब्ध नहीं") }
    var selectedVillage by remember { mutableStateOf<VillageRow?>(null) }
    var error by remember { mutableStateOf("") }; var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← वापस") }; Text("अपनी सेवा दर्ज करें", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("आपका गाँव GPS से अपने-आप चुना जाएगा।")
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("नाम") }, singleLine = true)
        AutoLocationSelector { selectedVillage = it; village = it.name }
        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, Modifier.fillMaxWidth(), label = { Text("मोबाइल नंबर") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Text("सेवा चुनें", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        categories.forEach { c -> OutlinedButton(onClick = { service = c.title }, Modifier.fillMaxWidth()) { Text(if (service == c.title) "✓ ${c.emoji} ${c.title}" else "${c.emoji} ${c.title}") } }
        OutlinedButton(onClick = { available = !available }, Modifier.fillMaxWidth()) { Text(if (available) "🟢 अभी उपलब्ध" else "⚪ अभी उपलब्ध नहीं") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (!saved) Button(onClick = {
            error = when {
                name.isBlank() -> "नाम भरें।"
                selectedVillage == null && village.isBlank() -> "पहले गाँव चुनें।"
                phone.length != 10 -> "मोबाइल नंबर 10 अंकों का होना चाहिए।"
                else -> ""
            }
            if (error.isBlank()) {
                saveProvider(context, Provider(name.trim(), village.ifBlank { selectedVillage?.name.orEmpty() }, service, if (available) "अभी उपलब्ध" else "अभी उपलब्ध नहीं", phone))
                saved = true
            }
        }, Modifier.fillMaxWidth()) { Text("प्रोफाइल सेव करें") }
        if (saved) { Text("✓ प्रोफाइल सेव हो गई।"); OutlinedButton(onClick = onSaved, Modifier.fillMaxWidth()) { Text("होम पर जाएँ") } }
    }
}

@Composable
private fun ServiceResultsScreen(category: Category, categories: List<Category>, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedVillage by remember { mutableStateOf<VillageRow?>(null) }
    var providers by remember { mutableStateOf<List<SupabaseProviderRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(selectedVillage?.id, category.id) {
        val serviceId = category.id
        val villageId = selectedVillage?.id
        if (serviceId == null || villageId == null) { providers = emptyList(); return@LaunchedEffect }
        loading = true; error = ""
        runCatching { SupabaseRepository.getActiveProviders(serviceId, villageId) }
            .onSuccess { providers = it; loading = false }
            .onFailure { providers = emptyList(); error = "सेवा प्रदाता लोड नहीं हो सके: "+(it.message ?: "नेटवर्क समस्या"); loading = false }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← वापस") }
        Text(category.emoji+" "+category.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        AutoLocationSelector { selectedVillage = it }
        when {
            selectedVillage == null -> Text("पहले गाँव चुनें।")
            loading -> CircularProgressIndicator()
            error.isNotBlank() -> Text(error, color = MaterialTheme.colorScheme.error)
            providers.isEmpty() -> Text("इस गाँव में अभी कोई सक्रिय सेवा प्रदाता नहीं मिला।")
            else -> providers.forEach { provider ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("सेवा प्रदाता", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("📍 "+selectedVillage?.name.orEmpty())
                        Text(when (provider.availability) { "available_now" -> "🟢 अभी उपलब्ध"; "available_today" -> "🟡 आज उपलब्ध"; else -> "⚪ अभी उपलब्ध नहीं" })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { dial(context, provider.phone) }, Modifier.weight(1f)) { Text("📞 Call") }
                            OutlinedButton(onClick = { whatsapp(context, provider.phone, "सेवा प्रदाता") }, Modifier.weight(1f)) { Text("💬 WhatsApp") }
                        }
                    }
                }
            }
        }
    }
}
