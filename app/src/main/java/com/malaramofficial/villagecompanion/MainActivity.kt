package com.malaramofficial.villagecompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VillageCompanionApp() }
    }
}

private data class Category(val emoji: String, val title: String, val subtitle: String)

private val categories = listOf(
    Category("🌾", "कृषि मजदूर", "खेत का काम"),
    Category("🚜", "Tractor / मशीन", "किराये पर मशीन"),
    Category("⚡", "Electrician", "बिजली का काम"),
    Category("⚙️", "Motor / Pump", "मरम्मत सेवा"),
    Category("🚰", "Plumber", "पानी की लाइन"),
    Category("🧱", "Mason / Welding", "निर्माण काम")
)

@Composable
fun VillageCompanionApp() {
    var selectedMode by remember { mutableStateOf("customer") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌾", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Village Companion",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "गाँव साथी • काम और सेवा",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "गाँव में क्या चाहिए?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("अपने गाँव के आसपास उपलब्ध व्यक्ति या मशीन खोजें।")
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { selectedMode = "customer" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🙋 मुझे सेवा चाहिए")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { selectedMode = "provider" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("👨‍🔧 मैं सेवा देता हूँ")
                        }
                    }
                }

                Text(
                    if (selectedMode == "customer") "लोकप्रिय सेवाएँ" else "आप कौन-सी सेवा देते हैं?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { category ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            onClick = { }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(category.emoji, style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    category.title,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    category.subtitle,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Text(
                    "अगला चरण: गाँव चुनें → उपलब्ध लोग देखें → Call / WhatsApp",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
