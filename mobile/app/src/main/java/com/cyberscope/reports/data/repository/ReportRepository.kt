package com.cyberscope.reports.data.repository

import com.cyberscope.reports.data.api.ApiClient
import com.cyberscope.reports.data.model.Scan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

class ReportRepository {
    
    private val api = ApiClient.api
    
    suspend fun getAllScans(): Result<List<Scan>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllScans()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.scans)
            } else {
                Result.failure(Exception("Failed to fetch scans: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getReports(): Result<List<Scan>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReports()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.reports)
            } else {
                Result.failure(Exception("Failed to fetch reports: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getScanDetails(scanId: String): Result<Scan> = withContext(Dispatchers.IO) {
        try {
            val response = api.getScanStatus(scanId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch scan details: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun downloadPdfReport(scanId: String): Result<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadPdfReport(scanId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to download PDF: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun downloadHtmlReport(scanId: String): Result<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val response = api.downloadHtmlReport(scanId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to download HTML: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
