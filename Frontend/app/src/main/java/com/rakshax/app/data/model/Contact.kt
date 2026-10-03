package com.rakshax.app.data.model

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val priority: Int = 1, // 1: Immediate, 2: Secondary, 3: Fallback
    val isEnabled: Boolean = true,
    val relation: String = "Family",
    val acknowledged: Boolean = false
)
