package com.jeysi.gogetcash

// A universal data class to hold any type of transaction
data class AnyTransaction(
    val id: String,
    val type: String, // e.g., "Loan", "Cash In", "Cash Out"
    val name: String,
    val amount: Double,
    val fee: Double,
    val timestamp: Long,
    val isPaid: Boolean? = null // Specific to Loans
)
