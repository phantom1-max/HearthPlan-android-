package com.example.data

import com.example.model.*

object BuiltinTemplates {

    fun getTemplates(): List<Project> {
        return listOf(
            createNordicVillaTemplate(),
            createUrbanApartmentTemplate(),
            createCompactStudioTemplate()
        )
    }

    private fun createNordicVillaTemplate(): Project {
        val groundRooms = listOf(
            RoomItem(
                id = "nv_g_living",
                name = "Living Room",
                type = RoomType.LIVING_ROOM,
                x = 1.0f,
                y = 1.0f,
                length = 6.0f,
                breadth = 5.0f,
                doors = listOf(DoorOpening("d1", WallEdge.BOTTOM, 2.5f, 1.0f, true)),
                windows = listOf(WindowOpening("w1", WallEdge.LEFT, 1.5f, 2.0f), WindowOpening("w2", WallEdge.TOP, 2.0f, 1.8f))
            ),
            RoomItem(
                id = "nv_g_dining",
                name = "Dining & Kitchen",
                type = RoomType.KITCHEN,
                x = 7.5f,
                y = 1.0f,
                length = 5.5f,
                breadth = 4.0f,
                doors = listOf(DoorOpening("d2", WallEdge.BOTTOM, 1.5f, 0.9f, true)),
                windows = listOf(WindowOpening("w3", WallEdge.RIGHT, 1.2f, 1.5f))
            ),
            RoomItem(
                id = "nv_g_bath",
                name = "Powder Room",
                type = RoomType.BATHROOM,
                x = 7.5f,
                y = 5.5f,
                length = 2.5f,
                breadth = 2.2f,
                doors = listOf(DoorOpening("d3", WallEdge.LEFT, 0.6f, 0.8f, true))
            ),
            RoomItem(
                id = "nv_g_study",
                name = "Home Office",
                type = RoomType.STUDY,
                x = 1.0f,
                y = 6.5f,
                length = 4.0f,
                breadth = 3.5f,
                doors = listOf(DoorOpening("d4", WallEdge.TOP, 1.2f, 0.9f, true)),
                windows = listOf(WindowOpening("w4", WallEdge.BOTTOM, 1.2f, 1.4f))
            )
        )

        val groundFurniture = listOf(
            FurnitureItem("gf_sofa", "sofa_lshape", "L-Shaped Sectional", FurnitureCategory.SOFAS_CHAIRS, 1.8f, 1.8f, 2.6f, 1.8f),
            FurnitureItem("gf_coffee", "coffee_table", "Coffee Table", FurnitureCategory.TABLES_DESKS, 2.6f, 3.8f, 1.1f, 0.6f),
            FurnitureItem("gf_tv", "tv_unit", "Media Console", FurnitureCategory.STORAGE, 1.5f, 5.3f, 1.8f, 0.45f),
            FurnitureItem("gf_dining", "dining_table_6", "Dining Table (6 Seater)", FurnitureCategory.TABLES_DESKS, 8.5f, 1.8f, 1.8f, 0.9f),
            FurnitureItem("gf_kitchen", "kitchen_counter", "Modular Counter", FurnitureCategory.KITCHEN, 10.8f, 1.2f, 2.0f, 0.6f, 90f),
            FurnitureItem("gf_desk", "study_desk", "Study / Work Desk", FurnitureCategory.TABLES_DESKS, 1.5f, 7.0f, 1.4f, 0.7f),
            FurnitureItem("gf_toilet", "toilet", "Toilet / WC", FurnitureCategory.BATHROOM, 8.0f, 6.0f, 0.5f, 0.7f)
        )

        val firstFloorRooms = listOf(
            RoomItem(
                id = "nv_f_master",
                name = "Master Suite",
                type = RoomType.BEDROOM,
                x = 1.0f,
                y = 1.0f,
                length = 5.0f,
                breadth = 4.5f,
                doors = listOf(DoorOpening("d5", WallEdge.BOTTOM, 1.5f, 0.9f, true)),
                windows = listOf(WindowOpening("w5", WallEdge.TOP, 1.5f, 1.8f))
            ),
            RoomItem(
                id = "nv_f_bed2",
                name = "Bedroom 2",
                type = RoomType.BEDROOM,
                x = 6.5f,
                y = 1.0f,
                length = 4.2f,
                breadth = 3.8f,
                doors = listOf(DoorOpening("d6", WallEdge.BOTTOM, 1.0f, 0.9f, true)),
                windows = listOf(WindowOpening("w6", WallEdge.TOP, 1.2f, 1.4f))
            ),
            RoomItem(
                id = "nv_f_bath",
                name = "Family Bathroom",
                type = RoomType.BATHROOM,
                x = 6.5f,
                y = 5.2f,
                length = 3.5f,
                breadth = 2.6f,
                doors = listOf(DoorOpening("d7", WallEdge.LEFT, 0.8f, 0.8f, true))
            ),
            RoomItem(
                id = "nv_f_balcony",
                name = "Terrace Balcony",
                type = RoomType.BALCONY,
                x = 1.0f,
                y = 6.0f,
                length = 4.5f,
                breadth = 2.2f,
                doors = listOf(DoorOpening("d8", WallEdge.TOP, 2.0f, 1.2f, true))
            )
        )

        val firstFloorFurniture = listOf(
            FurnitureItem("ff_bed1", "bed_king", "King Size Bed", FurnitureCategory.BEDS, 2.2f, 1.5f, 2.0f, 2.1f),
            FurnitureItem("ff_ward1", "wardrobe_3door", "3-Door Wardrobe", FurnitureCategory.STORAGE, 1.2f, 4.2f, 1.8f, 0.6f),
            FurnitureItem("ff_bed2", "bed_queen", "Queen Size Bed", FurnitureCategory.BEDS, 7.2f, 1.5f, 1.6f, 2.0f),
            FurnitureItem("ff_tub", "bathtub", "Bathtub", FurnitureCategory.BATHROOM, 7.0f, 5.5f, 1.7f, 0.75f)
        )

        return Project(
            id = "template_nordic_villa",
            name = "Nordic Family Villa",
            unit = MeasurementUnit.METERS,
            plotLength = 15.0f,
            plotBreadth = 12.0f,
            floors = listOf(
                Floor("nv_f0", "Ground Floor", 0, 14.0f, 11.0f, groundRooms, groundFurniture),
                Floor("nv_f1", "First Floor", 1, 12.0f, 10.0f, firstFloorRooms, firstFloorFurniture)
            ),
            isBuiltinTemplate = true,
            description = "Spacious two-storey family residence with open-concept living, ground study, master suite with terrace balcony.",
            author = "Hearthplan Studio"
        )
    }

    private fun createUrbanApartmentTemplate(): Project {
        val rooms = listOf(
            RoomItem(
                id = "ua_living",
                name = "Living & Dining",
                type = RoomType.LIVING_ROOM,
                x = 1.0f,
                y = 1.0f,
                length = 5.5f,
                breadth = 4.2f,
                doors = listOf(DoorOpening("ua_d1", WallEdge.BOTTOM, 1.0f, 0.9f, true)),
                windows = listOf(WindowOpening("ua_w1", WallEdge.TOP, 1.5f, 2.0f))
            ),
            RoomItem(
                id = "ua_kitchen",
                name = "Kitchen",
                type = RoomType.KITCHEN,
                x = 7.0f,
                y = 1.0f,
                length = 3.8f,
                breadth = 3.0f,
                doors = listOf(DoorOpening("ua_d2", WallEdge.LEFT, 1.0f, 0.85f, true)),
                windows = listOf(WindowOpening("ua_w2", WallEdge.TOP, 1.0f, 1.2f))
            ),
            RoomItem(
                id = "ua_master",
                name = "Master Bedroom",
                type = RoomType.BEDROOM,
                x = 1.0f,
                y = 5.8f,
                length = 4.5f,
                breadth = 3.8f,
                doors = listOf(DoorOpening("ua_d3", WallEdge.RIGHT, 1.2f, 0.9f, true)),
                windows = listOf(WindowOpening("ua_w3", WallEdge.BOTTOM, 1.2f, 1.6f))
            ),
            RoomItem(
                id = "ua_guest",
                name = "Guest Bedroom",
                type = RoomType.BEDROOM,
                x = 6.0f,
                y = 5.8f,
                length = 3.6f,
                breadth = 3.2f,
                doors = listOf(DoorOpening("ua_d4", WallEdge.LEFT, 1.0f, 0.85f, true)),
                windows = listOf(WindowOpening("ua_w4", WallEdge.BOTTOM, 1.0f, 1.2f))
            ),
            RoomItem(
                id = "ua_bath",
                name = "Bathroom",
                type = RoomType.BATHROOM,
                x = 7.0f,
                y = 4.3f,
                length = 2.8f,
                breadth = 1.8f,
                doors = listOf(DoorOpening("ua_d5", WallEdge.LEFT, 0.5f, 0.75f, true))
            )
        )

        val furniture = listOf(
            FurnitureItem("ua_f_sofa", "sofa_3seater", "3-Seater Sofa", FurnitureCategory.SOFAS_CHAIRS, 1.8f, 1.8f, 2.2f, 0.9f),
            FurnitureItem("ua_f_table", "dining_table_4", "Dining Table (4 Seater)", FurnitureCategory.TABLES_DESKS, 4.5f, 2.8f, 1.2f, 0.8f),
            FurnitureItem("ua_f_bed1", "bed_queen", "Queen Size Bed", FurnitureCategory.BEDS, 1.8f, 6.5f, 1.6f, 2.0f),
            FurnitureItem("ua_f_bed2", "bed_single", "Single Bed", FurnitureCategory.BEDS, 6.8f, 6.5f, 1.0f, 1.9f),
            FurnitureItem("ua_f_sink", "kitchen_sink", "Kitchen Sink", FurnitureCategory.KITCHEN, 7.5f, 1.2f, 1.0f, 0.5f)
        )

        return Project(
            id = "template_urban_apartment",
            name = "Modern 2-BHK Apartment",
            unit = MeasurementUnit.METERS,
            plotLength = 12.0f,
            plotBreadth = 11.0f,
            floors = listOf(
                Floor("ua_f0", "Ground Floor", 0, 11.0f, 10.2f, rooms, furniture)
            ),
            isBuiltinTemplate = true,
            description = "Efficient urban flat layout with master and guest bedrooms, practical semi-open kitchen, and centralized dining.",
            author = "Hearthplan Studio"
        )
    }

    private fun createCompactStudioTemplate(): Project {
        val rooms = listOf(
            RoomItem(
                id = "cs_main",
                name = "Studio Living & Bed",
                type = RoomType.LIVING_ROOM,
                x = 1.0f,
                y = 1.0f,
                length = 5.0f,
                breadth = 4.5f,
                doors = listOf(DoorOpening("cs_d1", WallEdge.BOTTOM, 1.5f, 0.9f, true)),
                windows = listOf(WindowOpening("cs_w1", WallEdge.LEFT, 1.2f, 1.8f))
            ),
            RoomItem(
                id = "cs_kitchen",
                name = "Kitchenette",
                type = RoomType.KITCHEN,
                x = 6.4f,
                y = 1.0f,
                length = 2.5f,
                breadth = 2.2f,
                doors = listOf(DoorOpening("cs_d2", WallEdge.LEFT, 0.5f, 0.8f, true))
            ),
            RoomItem(
                id = "cs_bath",
                name = "Compact Bath",
                type = RoomType.BATHROOM,
                x = 6.4f,
                y = 3.5f,
                length = 2.5f,
                breadth = 2.0f,
                doors = listOf(DoorOpening("cs_d3", WallEdge.LEFT, 0.5f, 0.75f, true))
            )
        )

        val furniture = listOf(
            FurnitureItem("cs_bed", "bed_queen", "Queen Size Bed", FurnitureCategory.BEDS, 1.5f, 1.5f, 1.6f, 2.0f),
            FurnitureItem("cs_desk", "study_desk", "Study / Work Desk", FurnitureCategory.TABLES_DESKS, 3.8f, 1.5f, 1.4f, 0.7f),
            FurnitureItem("cs_sofa", "sofa_2seater", "2-Seater Loveseat", FurnitureCategory.SOFAS_CHAIRS, 2.0f, 3.8f, 1.5f, 0.85f)
        )

        return Project(
            id = "template_compact_studio",
            name = "Compact Eco Studio",
            unit = MeasurementUnit.METERS,
            plotLength = 10.0f,
            plotBreadth = 7.0f,
            floors = listOf(
                Floor("cs_f0", "Ground Floor", 0, 9.2f, 6.0f, rooms, furniture)
            ),
            isBuiltinTemplate = true,
            description = "Minimalist and light-filled studio apartment optimized for single living, student housing, or modular builds.",
            author = "Hearthplan Studio"
        )
    }
}
