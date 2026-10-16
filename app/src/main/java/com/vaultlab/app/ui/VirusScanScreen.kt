package com.vaultlab.app.ui

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vaultlab.app.R
import com.vaultlab.app.data.ApiException
import com.vaultlab.app.data.SecureStore
import com.vaultlab.app.data.VirusTotalApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun VirusScanScreen(store: SecureStore, onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<VirusTotalApi.Stats?>(null) }
    var resultLabel by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val noKey = stringResource(R.string.vt_no_key)
    val invalidKey = stringResource(R.string.vt_invalid_key)
    val rateLimit = stringResource(R.string.vt_rate_limit)
    val genericError = stringResource(R.string.error_generic)
    val tooBig = stringResource(R.string.vt_too_big)

    fun runScan(label: String, work: suspend (apiKey: String) -> VirusTotalApi.Stats) {
        val key = store.getVirusTotalKey()
        if (key.isNullOrBlank()) {
            error = noKey
            return
        }
        busy = true
        error = null
        result = null
        resultLabel = label
        scope.launch {
            try {
                result = work(key)
            } catch (e: ApiException) {
                error = when (e.code) {
                    401, 403 -> invalidKey
                    429 -> rateLimit
                    else -> genericError
                }
            } catch (e: Exception) {
                error = genericError
            } finally {
                busy = false
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (queryLong(context, uri, OpenableColumns.SIZE) > VirusTotalApi.MAX_FILE_BYTES) {
            error = tooBig
            return@rememberLauncherForActivityResult
        }
        val name = queryString(context, uri, OpenableColumns.DISPLAY_NAME) ?: "file"
        runScan(name) { apiKey ->
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("cannot read file")
            }
            VirusTotalApi.scanFile(apiKey, name, bytes)
        }
    }

    ToolScaffold(title = stringResource(R.string.vt_title), onBack = onBack) {
        if (store.getVirusTotalKey().isNullOrBlank()) {
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.tool_settings)) }
        }

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.vt_url_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val target = url.trim().let { if (it.startsWith("http")) it else "https://$it" }
                runScan(target) { apiKey -> VirusTotalApi.scanUrl(apiKey, target) }
            },
            enabled = !busy && url.isNotBlank(),
        ) {
            Text(stringResource(R.string.vt_scan_url))
        }

        Spacer(Modifier.height(4.dp))

        OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, enabled = !busy) {
            Text(stringResource(R.string.vt_pick_file))
        }
        Text(stringResource(R.string.vt_file_limit), style = MaterialTheme.typography.bodySmall)

        if (busy) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
                Text(stringResource(R.string.vt_scanning), style = MaterialTheme.typography.bodySmall)
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        result?.let { stats ->
            val dangerous = stats.malicious + stats.suspicious > 0
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(resultLabel, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                    Text(
                        stringResource(if (dangerous) R.string.vt_dangerous else R.string.vt_clean),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (dangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(
                            R.string.vt_stats,
                            stats.malicious,
                            stats.suspicious,
                            stats.harmless,
                            stats.undetected,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun queryLong(context: Context, uri: Uri, column: String): Long {
    val cursor: Cursor? = context.contentResolver.query(uri, arrayOf(column), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(column)
            if (index >= 0 && !it.isNull(index)) return it.getLong(index)
        }
    }
    return -1L
}

private fun queryString(context: Context, uri: Uri, column: String): String? {
    val cursor: Cursor? = context.contentResolver.query(uri, arrayOf(column), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(column)
            if (index >= 0) return it.getString(index)
        }
    }
    return null
}
