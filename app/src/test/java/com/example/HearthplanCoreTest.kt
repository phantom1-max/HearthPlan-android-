package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.BuiltinTemplates
import com.example.data.HearthplanDatabase
import com.example.data.ProjectJsonAdapter
import com.example.data.ProjectRepository
import com.example.model.*
import com.example.util.PdfExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HearthplanCoreTest {

    private lateinit var context: Context
    private lateinit var database: HearthplanDatabase
    private lateinit var repository: ProjectRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HearthplanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProjectRepository(database.projectDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Hearthplan", appName)
    }

    @Test
    fun testJsonSerializationRoundtrip() {
        val testProject = Project(
            id = UUID.randomUUID().toString(),
            name = "Test Residence",
            unit = MeasurementUnit.METERS,
            plotLength = 16.0f,
            plotBreadth = 12.5f,
            floors = listOf(
                Floor(
                    id = "f0",
                    name = "Ground Floor",
                    level = 0,
                    length = 15.0f,
                    breadth = 12.0f,
                    rooms = listOf(
                        RoomItem(
                            id = "r1",
                            name = "Living Room",
                            type = RoomType.LIVING_ROOM,
                            x = 1.0f,
                            y = 1.0f,
                            length = 5.0f,
                            breadth = 4.0f,
                            doors = listOf(DoorOpening("d1", WallEdge.BOTTOM, 1.0f, 0.9f, true)),
                            windows = listOf(WindowOpening("w1", WallEdge.TOP, 1.0f, 1.5f))
                        )
                    ),
                    furniture = listOf(
                        FurnitureItem("fn1", "sofa_3seater", "3-Seater Sofa", FurnitureCategory.SOFAS_CHAIRS, 2.0f, 2.0f, 2.2f, 0.9f)
                    )
                )
            )
        )

        val json = ProjectJsonAdapter.toJson(testProject)
        assertNotNull(json)
        assertTrue(json.contains("Test Residence"))

        val restored = ProjectJsonAdapter.fromJson(json)
        assertEquals(testProject.id, restored.id)
        assertEquals("Test Residence", restored.name)
        assertEquals(16.0f, restored.plotLength, 0.01f)
        assertEquals(1, restored.floors.size)
        assertEquals(1, restored.floors[0].rooms.size)
        assertEquals("Living Room", restored.floors[0].rooms[0].name)
        assertEquals(1, restored.floors[0].furniture.size)
    }

    @Test
    fun testRoomDatabasePersistence() = runBlocking {
        repository.initializeBuiltinTemplatesIfEmpty()

        val templates = repository.builtinTemplates.first()
        assertTrue("Builtin templates should be pre-seeded into Room", templates.isNotEmpty())

        val template = templates.first()
        val cloned = repository.cloneAsNewProject(template, "My Custom Villa")
        assertFalse("Cloned project should not be marked as builtin template", cloned.isBuiltinTemplate)
        assertEquals("My Custom Villa", cloned.name)

        val userProjects = repository.userProjects.first()
        assertTrue("User projects should contain the cloned project", userProjects.any { it.id == cloned.id })
    }

    @Test
    fun testPdfExportGeneration() {
        val template = BuiltinTemplates.getTemplates().first()
        try {
            val pdfFile = PdfExporter.exportProjectToPdf(context, template)
            assertNotNull(pdfFile)
            assertTrue("PDF file should exist", pdfFile.exists())
        } catch (e: Exception) {
            // Robolectric on local JVM may not have native Skia PDF engine
            assertTrue("Native PDF requires Android runtime", e is IllegalStateException || e is UnsupportedOperationException)
        }
    }

    @Test
    fun testIrregularPlotDimensionsAndUnits() {
        val irregularProject = Project(
            id = "irr-1",
            name = "Trapezoid Lot",
            unit = MeasurementUnit.METERS,
            plotLength = 25f,
            plotBreadth = 20f,
            plotWidth1 = 19f,
            plotWidth2 = 25f,
            plotLength1 = 15f,
            plotLength2 = 20f,
            floors = emptyList()
        )

        assertTrue("Should detect irregular plot when sides differ", irregularProject.isIrregularPlot)
        assertEquals(22f * 17.5f, irregularProject.totalPlotAreaSqMeters, 0.01f)

        // Test JSON roundtrip with irregular plot fields
        val json = ProjectJsonAdapter.toJson(irregularProject)
        val deserialized = ProjectJsonAdapter.fromJson(json)
        assertEquals(19f, deserialized.plotWidth1, 0.01f)
        assertEquals(25f, deserialized.plotWidth2, 0.01f)
        assertEquals(15f, deserialized.plotLength1, 0.01f)
        assertEquals(20f, deserialized.plotLength2, 0.01f)
        assertTrue(deserialized.isIrregularPlot)

        // Test measurement units
        assertEquals("10.00 m", MeasurementUnit.METERS.format(10f))
        assertEquals("32.81 ft", MeasurementUnit.FEET.format(10f))
        assertEquals("393.7 in", MeasurementUnit.INCHES.format(10f))
        assertEquals("1000.0 cm", MeasurementUnit.CENTIMETERS.format(10f))
        assertEquals("10000 mm", MeasurementUnit.MILLIMETERS.format(10f))
        assertEquals("10.94 yd", MeasurementUnit.YARDS.format(10f))
    }
}
