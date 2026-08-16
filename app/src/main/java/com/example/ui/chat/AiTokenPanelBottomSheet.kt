package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTokenPanelBottomSheet(
    onDismiss: () -> Unit,
    viewModel: TokenPanelViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState()
    val rpm by viewModel.currentRpm.collectAsState()
    val tpm by viewModel.currentTpm.collectAsState()
    val totalCost by viewModel.totalCost.collectAsState()

    val currentTpmValue = tpm ?: 0
    val maxTpm = 60000 // example limit
    val tpmProgress = (currentTpmValue.toFloat() / maxTpm.toFloat()).coerceIn(0f, 1f)
    val isTpmWarning = tpmProgress > 0.8f

    val currentRpmValue = rpm
    val maxRpm = 60 // example limit
    val rpmProgress = (currentRpmValue.toFloat() / maxRpm.toFloat()).coerceIn(0f, 1f)
    val isRpmWarning = rpmProgress > 0.8f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "AI Token & Quota Health",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // TPM Bar
            Text("Tokens Per Minute (TPM)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { tpmProgress },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = if (isTpmWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("$currentTpmValue / $maxTpm")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // RPM Bar
            Text("Requests Per Minute (RPM)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { rpmProgress },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = if (isRpmWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("$currentRpmValue / $maxRpm")
            }
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Estimated Total Cost") },
                    supportingContent = { Text("Across all logged requests") },
                    leadingContent = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    trailingContent = { Text(String.format("$%.4f", totalCost ?: 0.0), style = MaterialTheme.typography.titleMedium) }
                )
            }
        }
    }
}
