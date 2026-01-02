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
