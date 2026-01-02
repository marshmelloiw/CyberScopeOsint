package com.cyberscope.reports.data.api

import com.cyberscope.reports.data.model.ReportsResponse
import com.cyberscope.reports.data.model.Scan
import com.cyberscope.reports.data.model.ScansResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CyberScopeApi {
    
    @GET("scans")
    suspend fun getAllScans(): Response<ScansResponse>
    
    @GET("scans/reports")
    suspend fun getReports(): Response<ReportsResponse>
    
    @GET("scans/status/{scanId}")
    suspend fun getScanStatus(@Path("scanId") scanId: String): Response<Scan>
    
    @GET("scans/{scanId}/report/pdf")
    suspend fun downloadPdfReport(@Path("scanId") scanId: String): Response<ResponseBody>
    
    @GET("scans/{scanId}/report/html")
    suspend fun downloadHtmlReport(@Path("scanId") scanId: String): Response<ResponseBody>
}
