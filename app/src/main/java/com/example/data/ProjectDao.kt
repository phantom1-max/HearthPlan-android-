package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isBuiltinTemplate = 0 ORDER BY updatedAt DESC")
    fun getUserProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isBuiltinTemplate = 1 ORDER BY name ASC")
    fun getBuiltinTemplates(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("SELECT COUNT(*) FROM projects WHERE isBuiltinTemplate = 1")
    suspend fun countBuiltinTemplates(): Int
}
