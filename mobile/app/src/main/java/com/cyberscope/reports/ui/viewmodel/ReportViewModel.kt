package com.cyberscope.reports.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberscope.reports.data.model.Scan
import com.cyberscope.reports.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class ReportViewModel : ViewModel() {
    
    private val repository = ReportRepository()
    
    private val _scansState = MutableStateFlow<UiState<List<Scan>>>(UiState.Loading)
    val scansState: StateFlow<UiState<List<Scan>>> = _scansState.asStateFlow()
    
    private val _reportsState = MutableStateFlow<UiState<List<Scan>>>(UiState.Loading)
    val reportsState: StateFlow<UiState<List<Scan>>> = _reportsState.asStateFlow()
    
    private val _selectedScan = MutableStateFlow<Scan?>(null)
    val selectedScan: StateFlow<Scan?> = _selectedScan.asStateFlow()
    
    private val _downloadState = MutableStateFlow<String?>(null)
    val downloadState: StateFlow<String?> = _downloadState.asStateFlow()
    
    init {
        loadScans()
        loadReports()
    }
    
    fun loadScans() {
        viewModelScope.launch {
            _scansState.value = UiState.Loading
            repository.getAllScans()
                .onSuccess { scans ->
                    _scansState.value = UiState.Success(scans)
                }
                .onFailure { error ->
                    _scansState.value = UiState.Error(error.message ?: "Unknown error")
                }
        }
    }
    
    fun loadReports() {
        viewModelScope.launch {
            _reportsState.value = UiState.Loading
            repository.getReports()
                .onSuccess { reports ->
                    _reportsState.value = UiState.Success(reports)
                }
                .onFailure { error ->
                    _reportsState.value = UiState.Error(error.message ?: "Unknown error")
                }
        }
    }
    
    fun loadScanDetails(scanId: String) {
        viewModelScope.launch {
            repository.getScanDetails(scanId)
                .onSuccess { scan ->
                    _selectedScan.value = scan
                }
                .onFailure { error ->
                    // Handle error
                }
        }
    }
    
    fun selectScan(scan: Scan) {
        _selectedScan.value = scan
    }
    
    fun clearSelectedScan() {
        _selectedScan.value = null
    }
    
    fun downloadPdfReport(scanId: String, context: android.content.Context) {
        viewModelScope.launch {
            _downloadState.value = "Downloading PDF..."
            repository.downloadPdfReport(scanId)
                .onSuccess { responseBody ->
                    val fileName = com.cyberscope.reports.utils.FileDownloader.getFileNameFromScanId(scanId, "pdf")
                    val success = com.cyberscope.reports.utils.FileDownloader.downloadAndSaveFile(
                        context,
                        responseBody,
                        fileName,
                        "application/pdf"
                    )
                    if (success) {
                        _downloadState.value = "PDF saved to Downloads/$fileName"
                    } else {
                        _downloadState.value = "Failed to save PDF"
                    }
                }
                .onFailure { error ->
                    _downloadState.value = "Download failed: ${error.message}"
                }
        }
    }
    
    fun downloadHtmlReport(scanId: String, context: android.content.Context) {
        viewModelScope.launch {
            _downloadState.value = "Downloading HTML..."
            repository.downloadHtmlReport(scanId)
                .onSuccess { responseBody ->
                    val fileName = com.cyberscope.reports.utils.FileDownloader.getFileNameFromScanId(scanId, "html")
                    val success = com.cyberscope.reports.utils.FileDownloader.downloadAndSaveFile(
                        context,
                        responseBody,
                        fileName,
                        "text/html"
                    )
                    if (success) {
                        _downloadState.value = "HTML saved to Downloads/$fileName"
                    } else {
                        _downloadState.value = "Failed to save HTML"
                    }
                }
                .onFailure { error ->
                    _downloadState.value = "Download failed: ${error.message}"
                }
        }
    }
    
    fun clearDownloadState() {
        _downloadState.value = null
    }
    
    fun deleteReport(scanId: String) {
        viewModelScope.launch {
            repository.deleteReport(scanId)
                .onSuccess {
                    // Reload reports after deletion
                    loadReports()
                }
                .onFailure { error ->
                    // Handle error - could show a toast or error message
                }
        }
    }
}
