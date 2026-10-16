package com.vaultlab.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vaultlab.app.R
import com.vaultlab.app.data.MailTmApi
import com.vaultlab.app.data.TempMessage
import kotlinx.coroutines.launch

@Composable
fun TempMailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Saved across rotation so the inbox survives a screen rotation.
    var address by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }

    var messages by remember { mutableStateOf<List<TempMessage>>(emptyList()) }
    var openedBody by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val genericError = stringResource(R.string.error_generic)
    val copiedText = stringResource(R.string.copied)

    fun loadInbox() {
        if (token.isEmpty()) return
        busy = true
        error = null
        scope.launch {
            try {
                messages = MailTmApi.messages(token)
            } catch (e: Exception) {
                error = genericError
            } finally {
                busy = false
            }
        }
    }

    fun createAddress() {
        busy = true
        error = null
        openedBody = null
        scope.launch {
            try {
                val account = MailTmApi.createAccount()
                address = account.address
                token = account.token
                messages = MailTmApi.messages(account.token)
            } catch (e: Exception) {
                error = genericError
            } finally {
                busy = false
            }
        }
    }

    fun openMessage(message: TempMessage) {
        busy = true
        scope.launch {
            try {
                val body = MailTmApi.messageText(token, message.id)
                openedBody = "${message.subject}\n\n${body.ifBlank { message.intro }}"
            } catch (e: Exception) {
                error = genericError
            } finally {
                busy = false
            }
        }
    }

    ToolScaffold(title = stringResource(R.string.tempmail_title), onBack = onBack) {
        if (address.isEmpty()) {
            Text(stringResource(R.string.tempmail_intro))
            Button(onClick = { createAddress() }, enabled = !busy) {
                Text(stringResource(R.string.tempmail_create))
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.tempmail_address_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(address, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("email", address))
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        }) {
                            Text(stringResource(R.string.tempmail_copy))
                        }
                        OutlinedButton(onClick = { loadInbox() }, enabled = !busy) {
                            Text(stringResource(R.string.tempmail_refresh))
                        }
                        OutlinedButton(onClick = { createAddress() }, enabled = !busy) {
                            Text(stringResource(R.string.tempmail_create))
                        }
                    }
                }
            }
        }

        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        openedBody?.let { body ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(body)
                    TextButton(onClick = { openedBody = null }) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }

        if (address.isNotEmpty() && messages.isEmpty() && !busy) {
            Text(stringResource(R.string.tempmail_empty))
        }

        messages.forEach { message ->
            Card(onClick = { openMessage(message) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(message.from, style = MaterialTheme.typography.labelMedium)
                    Text(message.subject.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall)
                    Text(message.intro, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
