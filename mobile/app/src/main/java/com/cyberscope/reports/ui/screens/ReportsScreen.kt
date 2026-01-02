package com.cyberscope.reports.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberscope.reports.data.model.Scan
import com.cyberscope.reports.ui.viewmodel.ReportViewModel
import com.cyberscope.reports.ui.viewmodel.UiState

@Composable
fun ReportsScreen(viewModel: ReportViewModel) {
    val reportsState by viewModel.reportsState.collectAsState()
    val selectedScan by viewModel.selectedScan.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gemini Reports",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI-powered security analysis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { viewModel.loadReports() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }
        
        when (reportsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Success -> {
                val reports = (reportsState as UiState.Success<List<Scan>>).data
                if (reports.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No reports available",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Complete scans with Gemini analysis to see reports here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Sort reports by completion date (newest first)
                    val sortedReports = reports.sortedByDescending { 
                        it.completedAt ?: it.createdAt 
                    }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedReports) { report ->
                            ReportCard(
                                scan = report,
                                onClick = {
                                    viewModel.selectScan(report)
                                }
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: ${(reportsState as UiState.Error).message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadReports() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
    
    // Show report details dialog when scan is selected
    selectedScan?.let { scan ->
        ReportDetailsDialog(
            scan = scan,
            onDismiss = { viewModel.clearSelectedScan() }
        )
    }
}

@Composable
fun ReportCard(scan: Scan, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scan.name ?: "Report ${scan.scanId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan ID: ${scan.scanId.take(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                // Download button - will be handled in dialog
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download Report",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onClick)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Results summary
            scan.results?.let { results ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultSummaryChip(
                        label = "Providers",
                        value = results.size.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    val totalFindings = results.sumOf { it.findingsCount ?: 0 }
                    ResultSummaryChip(
                        label = "Findings",
                        value = totalFindings.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    val highestRisk = results.mapNotNull { it.riskLevel }.maxByOrNull { 
                        when(it.uppercase()) {
                            "CRITICAL" -> 4
                            "HIGH" -> 3
                            "MEDIUM" -> 2
                            "LOW" -> 1
                            else -> 0
                        }
                    }
                    highestRisk?.let {
                        RiskLevelChip(riskLevel = it)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Target info
            scan.targets?.firstOrNull()?.let { target ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Target: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = target,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Completed: ${formatDate(scan.completedAt ?: scan.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ResultSummaryChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun RiskLevelChip(riskLevel: String) {
    val color = when (riskLevel.uppercase()) {
        "CRITICAL" -> MaterialTheme.colorScheme.error
        "HIGH" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        "MEDIUM" -> MaterialTheme.colorScheme.tertiary
        "LOW" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = riskLevel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Risk",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailsDialog(scan: Scan, onDismiss: () -> Unit) {
    // Get ViewModel instance
    val viewModel: ReportViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val downloadState by viewModel.downloadState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Show toast for download state
    downloadState?.let { message ->
        LaunchedEffect(message) {
            // Toast would be shown here in a real implementation
            // For now, we'll just clear the state after showing
            kotlinx.coroutines.delay(2000)
            viewModel.clearDownloadState()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = scan.name ?: "Security Report",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Gemini AI Analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scan Info Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Scan Information",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailItem("Scan ID", scan.scanId.take(12))
                            DetailItem("Type", scan.type.uppercase())
                            DetailItem("Status", scan.status.uppercase())
                            scan.completedAt?.let {
                                DetailItem("Completed", formatDate(it))
                            }
                        }
                    }
                }
                
                // Download Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.downloadPdfReport(scan.scanId, context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF")
                        }
                        
                        Button(
                            onClick = { viewModel.downloadHtmlReport(scan.scanId, context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HTML")
                        }
                    }
                }
                
                // Download status message
                downloadState?.let { message ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.contains("success")) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                // Gemini AI Analysis Section
                scan.results?.let { results ->
                    results.forEach { result ->
                        result.geminiReport?.let { geminiReport ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🤖 Gemini AI Report",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            result.riskLevel?.let {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            when (it.uppercase()) {
                                                                "CRITICAL" -> MaterialTheme.colorScheme.error
                                                                "HIGH" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                                "MEDIUM" -> MaterialTheme.colorScheme.tertiary
                                                                else -> MaterialTheme.colorScheme.primary
                                                            }.copy(alpha = 0.2f)
                                                        )
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = it.uppercase(),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Text(
                                            text = "Provider: ${result.providerName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        result.findingsCount?.let {
                                            Text(
                                                text = "Findings: $it",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        
                                        result.riskScore?.let {
                                            Text(
                                                text = "Risk Score: $it/10",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider()
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Display Gemini Report Content
                                        geminiReport.forEach { (key, value) ->
                                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text(
                                                    text = key.replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = value.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Other Results (without Gemini report)
                scan.results?.let { results ->
                    val resultsWithoutGemini = results.filter { it.geminiReport == null }
                    if (resultsWithoutGemini.isNotEmpty()) {
                        item {
                            Text(
                                text = "Other Analysis Results",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        items(resultsWithoutGemini) { result ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = result.providerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        result.riskLevel?.let {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when (it.uppercase()) {
                                                            "CRITICAL" -> MaterialTheme.colorScheme.error
                                                            "HIGH" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                            "MEDIUM" -> MaterialTheme.colorScheme.tertiary
                                                            else -> MaterialTheme.colorScheme.primary
                                                        }.copy(alpha = 0.2f)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = it.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    result.findingsCount?.let {
                                        Text(
                                            text = "Findings: $it",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    result.riskScore?.let {
                                        Text(
                                            text = "Risk Score: $it/10",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
