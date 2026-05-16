package com.example.lostandfound.db

data class Item(
    val id: Long = 0L,
    val postType: String,
    val name: String,
    val phone: String,
    val description: String,
    val date: String,
    val location: String,
    val category: String,
    val imagePath: String?,
    val createdAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)
