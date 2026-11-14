package com.example.smarty.model

data class FridgeItem(
    val id: String = "",
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "item",
    val category: String = "",
    val expiryDate: String = "",
    val barcode: String = "",
    val imageUrl: String = ""
)