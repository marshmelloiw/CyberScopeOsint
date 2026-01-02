package com.cyberscope.reports.data.api

import com.cyberscope.reports.data.model.JwtResponse
import com.cyberscope.reports.data.model.LoginRequest
import com.cyberscope.reports.data.model.NotificationsResponse
import com.cyberscope.reports.data.model.ReportsResponse
import com.cyberscope.reports.data.model.Scan
import com.cyberscope.reports.data.model.ScansResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CyberScopeApi {
    
    // Auth endpoints
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<JwtResponse>
    
    // Scan endpoints
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
    
    @DELETE("scans/{scanId}")
    suspend fun deleteScan(@Path("scanId") scanId: String): Response<okhttp3.ResponseBody>
    
    // Notification endpoints
    @GET("notifications")
    suspend fun getNotifications(@retrofit2.http.Query("userId") userId: Long?): Response<NotificationsResponse>
    
    @retrofit2.http.PUT("notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Int): Response<okhttp3.ResponseBody>
}
