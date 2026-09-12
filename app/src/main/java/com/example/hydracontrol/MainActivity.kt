package com.example.hydracontrol

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.hydracontrol.data.HydraRepository
import com.example.hydracontrol.data.SessionManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repo: HydraRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(applicationContext)
        repo = HydraRepository(sessionManager)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HydraControlScreen(
                        sessionManager = sessionManager,
                        onAddDomain = { domain ->
                            lifecycleScope.launch {
                                val ok = repo.addDomain(domain)
                                val msg = if (ok) "Домен добавлен" else "Ошибка добавления (проверьте подключение)"
                                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSwitchServer = { serverId ->
                            lifecycleScope.launch {
                                val ok = repo.switchVpnServer(serverId)
                                val msg = if (ok) "Сервер изменен: $serverId" else "Ошибка смены сервера"
                                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydraControlScreen(
    sessionManager: SessionManager,
    onAddDomain: (String) -> Unit,
    onSwitchServer: (String) -> Unit
) {
    var domainInput by remember { mutableStateOf("") }
    var selectedServer by remember { mutableStateOf("NL-01") }
    val servers = listOf("NL-01 (Amsterdam)", "DE-01 (Frankfurt)", "US-01 (New York)", "FIN-01 (Helsinki)")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hydra Route Quick Control") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Маршрутизация доменов", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = domainInput,
                onValueChange = { domainInput = it },
                label = { Text("Домен (напр. rutracker.org)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (domainInput.isNotBlank()) {
                        onAddDomain(domainInput)
                        domainInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить в обход")
            }

            HorizontalDivider()

            Text("Активный VPN Outbound", style = MaterialTheme.typography.titleMedium)
            servers.forEach { server ->
                val serverKey = server.substringBefore(" ")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(server)
                    RadioButton(
                        selected = (selectedServer == serverKey),
                        onClick = {
                            selectedServer = serverKey
                            onSwitchServer(serverKey)
                        }
                    )
                }
            }
        }
    }
}
