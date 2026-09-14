package com.malaramofficial.villagecompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VillageCompanionApp() }
    }
}

private val categories = listOf(
    "🌾 कृषि मजदूर", "🚜 Tractor / कृषि मशीन", "⚡ Electrician",
    "⚙️ Motor / Pump Repair", "🚰 Plumber", "🧱 Mason / Welding"
)

@Composable
fun VillageCompanionApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Village Companion", style = MaterialTheme.typography.headlineMedium)
                Text("गाँव में काम और सेवा आसानी से खोजें।")

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { }) { Text("🙋 मुझे सेवा चाहिए") }
                    Button(onClick = { }) { Text("👨‍🔧 मैं सेवा देता हूँ") }
                }

                Text("लोकप्रिय सेवाएँ", style = MaterialTheme.typography.titleLarge)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { category ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(category, modifier = Modifier.padding(18.dp))
                        }
                    }
                }
            }
        }
    }
}
