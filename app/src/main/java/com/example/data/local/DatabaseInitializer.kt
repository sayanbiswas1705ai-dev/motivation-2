package com.example.data.local

import com.example.data.model.Category

object DatabaseInitializer {
    val defaultCategories = listOf(
        "Quant",
        "English Perfection",
        "English Practice",
        "Reasoning",
        "GA",
        "West Bengal Job",
        "Mock Test"
    )

    fun generateDefaultCategories(): List<Category> {
        return defaultCategories.map { Category(name = it) }
    }
}
