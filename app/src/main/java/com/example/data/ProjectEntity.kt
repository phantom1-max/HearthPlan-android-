package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unit: String,
    val plotLength: Float,
    val plotBreadth: Float,
    val jsonContent: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isBuiltinTemplate: Boolean = false,
    val description: String = ""
)
