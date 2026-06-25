package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocab_quiz_sets")
data class VocabQuizSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format "yyyy-MM-dd"
    val sourcePdfName: String
)

@Entity(tableName = "vocab_quiz_questions")
data class VocabQuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val quizSetId: Int,
    val word: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int,
    val explanation: String,
    val userSelectedOptionIndex: Int = -1,
    val isAnsweredCorrectly: Boolean = false,
    val isAnswered: Boolean = false
)
