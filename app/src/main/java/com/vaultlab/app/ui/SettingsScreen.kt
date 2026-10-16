package com.vaultlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vaultlab.app.R
import com.vaultlab.app.data.SecureStore

@Composable
fun SettingsScreen(store: SecureStore, onBack: () -> Unit) {
    var key by remember { mutableStateOf(store.getVirusTotalKey().orEmpty()) }
    var message by remember { mutableStateOf<String?>(null) }

    val savedText = stringResource(R.string.settings_saved)
    val clearedText = stringResource(R.string.settings_cleared)

    ToolScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        Text(stringResource(R.string.settings_vt_key), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.settings_vt_hint), style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                store.setVirusTotalKey(key)
                message = savedText
            }) {
                Text(stringResource(R.string.settings_save))
            }
            OutlinedButton(onClick = {
                key = ""
                store.setVirusTotalKey(null)
                message = clearedText
            }) {
                Text(stringResource(R.string.settings_clear))
            }
        }

        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
