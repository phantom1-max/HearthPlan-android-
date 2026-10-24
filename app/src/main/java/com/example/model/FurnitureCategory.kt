package com.example.model

enum class FurnitureCategory(val displayName: String) {
    BEDS("Beds"),
    SOFAS_CHAIRS("Sofas & Chairs"),
    TABLES_DESKS("Tables & Desks"),
    STORAGE("Wardrobes & Storage"),
    KITCHEN("Kitchen Appliances"),
    BATHROOM("Bathroom Fixtures"),
    LIGHTING("Lighting"),
    CUSTOM("Custom / AI Generated")
}

data class FurnitureTemplate(
    val templateId: String,
    val name: String,
    val category: FurnitureCategory,
    val defaultLengthMeters: Float,
    val defaultBreadthMeters: Float,
    val description: String = ""
)

object FurnitureCatalog {
    val items = listOf(
        // Beds
        FurnitureTemplate("bed_king", "King Size Bed", FurnitureCategory.BEDS, 2.0f, 2.1f, "200 × 210 cm double bed with headboard and pillows"),
        FurnitureTemplate("bed_queen", "Queen Size Bed", FurnitureCategory.BEDS, 1.6f, 2.0f, "160 × 200 cm standard queen bed"),
        FurnitureTemplate("bed_single", "Single Bed", FurnitureCategory.BEDS, 1.0f, 1.9f, "100 × 190 cm single twin bed"),
        FurnitureTemplate("bed_bunk", "Bunk Bed", FurnitureCategory.BEDS, 1.0f, 2.0f, "Compact two-tier bunk bed"),
        FurnitureTemplate("bed_side_table", "Nightstand / Bedside Table", FurnitureCategory.BEDS, 0.5f, 0.45f, "Compact bedside drawer table"),

        // Sofas & Chairs
        FurnitureTemplate("sofa_3seater", "3-Seater Sofa", FurnitureCategory.SOFAS_CHAIRS, 2.2f, 0.9f, "Spacious 3-seater living room couch"),
        FurnitureTemplate("sofa_2seater", "2-Seater Loveseat", FurnitureCategory.SOFAS_CHAIRS, 1.5f, 0.85f, "Comfortable two-seat sofa"),
        FurnitureTemplate("sofa_lshape", "L-Shaped Sectional", FurnitureCategory.SOFAS_CHAIRS, 2.6f, 1.8f, "Corner sectional sofa with chaise"),
        FurnitureTemplate("armchair", "Armchair", FurnitureCategory.SOFAS_CHAIRS, 0.85f, 0.85f, "Single upholstered reading armchair"),
        FurnitureTemplate("office_chair", "Office Ergonomic Chair", FurnitureCategory.SOFAS_CHAIRS, 0.65f, 0.65f, "Swivel desk chair with armrests"),

        // Tables & Desks
        FurnitureTemplate("dining_table_6", "Dining Table (6 Seater)", FurnitureCategory.TABLES_DESKS, 1.8f, 0.9f, "Rectangular dining table with 6 chairs"),
        FurnitureTemplate("dining_table_4", "Dining Table (4 Seater)", FurnitureCategory.TABLES_DESKS, 1.2f, 0.8f, "Compact 4-seater dining table"),
        FurnitureTemplate("coffee_table", "Coffee Table", FurnitureCategory.TABLES_DESKS, 1.1f, 0.6f, "Low living room coffee table"),
        FurnitureTemplate("study_desk", "Study / Work Desk", FurnitureCategory.TABLES_DESKS, 1.4f, 0.7f, "Computer desk with cable slot and drawer"),
        FurnitureTemplate("round_table", "Round Bistro Table", FurnitureCategory.TABLES_DESKS, 0.9f, 0.9f, "Circular bistro table"),

        // Storage
        FurnitureTemplate("wardrobe_3door", "3-Door Wardrobe", FurnitureCategory.STORAGE, 1.8f, 0.6f, "Full-height three-door bedroom wardrobe"),
        FurnitureTemplate("wardrobe_2door", "2-Door Wardrobe", FurnitureCategory.STORAGE, 1.2f, 0.6f, "Two-door closet"),
        FurnitureTemplate("bookshelf", "Tall Bookshelf", FurnitureCategory.STORAGE, 1.0f, 0.35f, "Multi-tier display bookshelf"),
        FurnitureTemplate("tv_unit", "Media / TV Console", FurnitureCategory.STORAGE, 1.8f, 0.45f, "Low-profile media entertainment stand"),
        FurnitureTemplate("dresser", "Dresser with Mirror", FurnitureCategory.STORAGE, 1.0f, 0.5f, "Chest of drawers"),

        // Kitchen
        FurnitureTemplate("kitchen_counter", "Modular Kitchen Counter", FurnitureCategory.KITCHEN, 2.4f, 0.6f, "Standard kitchen worktop with cabinets"),
        FurnitureTemplate("kitchen_sink", "Kitchen Double Sink", FurnitureCategory.KITCHEN, 1.0f, 0.5f, "Stainless steel double basin sink"),
        FurnitureTemplate("refrigerator", "Refrigerator", FurnitureCategory.KITCHEN, 0.8f, 0.75f, "Double-door modern fridge"),
        FurnitureTemplate("cooktop", "Gas Cooktop / Stove", FurnitureCategory.KITCHEN, 0.75f, 0.55f, "4-burner cooking range"),
        FurnitureTemplate("kitchen_island", "Kitchen Island", FurnitureCategory.KITCHEN, 1.8f, 0.9f, "Freestanding center prep island"),

        // Bathroom
        FurnitureTemplate("bathtub", "Bathtub", FurnitureCategory.BATHROOM, 1.7f, 0.75f, "Full-size acrylic soaking tub"),
        FurnitureTemplate("shower_cubicle", "Shower Enclosure", FurnitureCategory.BATHROOM, 0.9f, 0.9f, "Corner glass shower stall"),
        FurnitureTemplate("toilet", "Toilet / WC", FurnitureCategory.BATHROOM, 0.5f, 0.7f, "Floor-mounted porcelain water closet"),
        FurnitureTemplate("washbasin", "Vanity & Washbasin", FurnitureCategory.BATHROOM, 0.8f, 0.5f, "Bathroom wash vanity basin"),

        // Lighting
        FurnitureTemplate("floor_lamp", "Floor Standing Lamp", FurnitureCategory.LIGHTING, 0.4f, 0.4f, "Corner reading arc lamp"),
        FurnitureTemplate("ceiling_fan", "Ceiling Fan", FurnitureCategory.LIGHTING, 1.2f, 1.2f, "3-blade ceiling fan outline")
    )
}
