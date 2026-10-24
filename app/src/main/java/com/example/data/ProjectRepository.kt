package com.example.data

import com.example.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepository(private val projectDao: ProjectDao) {

    val userProjects: Flow<List<Project>> = projectDao.getUserProjects().map { list ->
        list.mapNotNull { entity ->
            try {
                ProjectJsonAdapter.fromJson(entity.jsonContent)
            } catch (_: Exception) {
                null
            }
        }
    }

    val builtinTemplates: Flow<List<Project>> = projectDao.getBuiltinTemplates().map { list ->
        list.mapNotNull { entity ->
            try {
                ProjectJsonAdapter.fromJson(entity.jsonContent)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun initializeBuiltinTemplatesIfEmpty() = withContext(Dispatchers.IO) {
        val count = projectDao.countBuiltinTemplates()
        if (count == 0) {
            val templates = BuiltinTemplates.getTemplates()
            for (tmpl in templates) {
                saveProject(tmpl)
            }
        }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val json = ProjectJsonAdapter.toJson(project)
        val entity = ProjectEntity(
            id = project.id,
            name = project.name,
            unit = project.unit.name,
            plotLength = project.plotLength,
            plotBreadth = project.plotBreadth,
            jsonContent = json,
            createdAt = project.createdAt,
            updatedAt = System.currentTimeMillis(),
            isBuiltinTemplate = project.isBuiltinTemplate,
            description = project.description
        )
        projectDao.insertProject(entity)
    }

    suspend fun getProjectById(id: String): Project? = withContext(Dispatchers.IO) {
        val entity = projectDao.getProjectById(id) ?: return@withContext null
        try {
            ProjectJsonAdapter.fromJson(entity.jsonContent)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }

    suspend fun cloneAsNewProject(source: Project, newName: String? = null): Project = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        val copy = source.copy(
            id = newId,
            name = newName ?: "${source.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isBuiltinTemplate = false
        )
        saveProject(copy)
        copy
    }

    fun importProjectFromJson(jsonStr: String): Result<Project> {
        return try {
            val imported = ProjectJsonAdapter.fromJson(jsonStr)
            val newProject = imported.copy(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isBuiltinTemplate = false
            )
            Result.success(newProject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportProjectToJson(project: Project): String {
        return ProjectJsonAdapter.toJson(project)
    }
}
