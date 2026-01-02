package com.cyberscope.reports.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class Scan(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("scanId")
    val scanId: String,
    
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("priority")
    val priority: String?,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("startedAt")
    val startedAt: String?,
    
    @SerializedName("completedAt")
    val completedAt: String?,
    
    @SerializedName("errorMessage")
    val errorMessage: String?,
    
    @SerializedName("targets")
    val targets: List<String>? = emptyList(),
    
    @SerializedName("results")
    val results: List<ScanResult>? = emptyList()
)

data class ScanResult(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("providerName")
    val providerName: String,
    
    @SerializedName("resultData")
    val resultData: Map<String, Any>,
    
    @SerializedName("riskScore")
    val riskScore: BigDecimal?,
    
    @SerializedName("riskLevel")
    val riskLevel: String?,
    
    @SerializedName("findingsCount")
    val findingsCount: Int?,
    
    @SerializedName("geminiReport")
    val geminiReport: Map<String, Any>?,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
)

data class ScansResponse(
    @SerializedName("scans")
    val scans: List<Scan>,
    
    @SerializedName("total")
    val total: Int
)

data class ReportsResponse(
    @SerializedName("reports")
    val reports: List<Scan>,
    
    @SerializedName("total")
    val total: Int
)

// Auth Models
data class LoginRequest(
    val email: String,
    val password: String
)

data class JwtResponse(
    @SerializedName("accessToken")
    val accessToken: String?,
    
    @SerializedName("refreshToken")
    val refreshToken: String?,
    
    @SerializedName("tokenType")
    val tokenType: String?,
    
    @SerializedName("userId")
    val userId: Long?,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("fullName")
    val fullName: String?,
    
    @SerializedName("role")
    val role: String?,
    
    @SerializedName("verified")
    val verified: Boolean?,
    
    @SerializedName("mfaRequired")
    val mfaRequired: Boolean?,
    
    @SerializedName("mfaEnabled")
    val mfaEnabled: Boolean?,
    
    @SerializedName("expiresIn")
    val expiresIn: Long?
)

data class Notification(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("userId")
    val userId: Long,
    
    @SerializedName("scanId")
    val scanId: Long,
    
    @SerializedName("scanIdString")
    val scanIdString: String?,
    
    @SerializedName("riskScore")
    val riskScore: BigDecimal?,
    
    @SerializedName("riskLevel")
    val riskLevel: String?,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("isRead")
    val isRead: Boolean,
    
    @SerializedName("createdAt")
    val createdAt: String
)

data class NotificationsResponse(
    @SerializedName("notifications")
    val notifications: List<Notification>,
    
    @SerializedName("unreadCount")
    val unreadCount: Int
)
