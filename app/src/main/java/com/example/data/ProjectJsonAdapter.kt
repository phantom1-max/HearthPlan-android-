package com.example.data

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

object ProjectJsonAdapter {

    fun toJson(project: Project): String {
        val root = JSONObject()
        root.put("id", project.id)
        root.put("name", project.name)
        root.put("unit", project.unit.name)
        root.put("plotLength", project.plotLength.toDouble())
        root.put("plotBreadth", project.plotBreadth.toDouble())
        root.put("plotWidth1", project.plotWidth1.toDouble())
        root.put("plotWidth2", project.plotWidth2.toDouble())
        root.put("plotLength1", project.plotLength1.toDouble())
        root.put("plotLength2", project.plotLength2.toDouble())
        root.put("createdAt", project.createdAt)
        root.put("updatedAt", project.updatedAt)
        root.put("isBuiltinTemplate", project.isBuiltinTemplate)
        root.put("description", project.description)
        root.put("author", project.author)

        val floorsArray = JSONArray()
        for (floor in project.floors) {
            val fObj = JSONObject()
            fObj.put("id", floor.id)
            fObj.put("name", floor.name)
            fObj.put("level", floor.level)
            fObj.put("length", floor.length.toDouble())
            fObj.put("breadth", floor.breadth.toDouble())

            val roomsArray = JSONArray()
            for (room in floor.rooms) {
                val rObj = JSONObject()
                rObj.put("id", room.id)
                rObj.put("name", room.name)
                rObj.put("type", room.type.name)
                rObj.put("x", room.x.toDouble())
                rObj.put("y", room.y.toDouble())
                rObj.put("length", room.length.toDouble())
                rObj.put("breadth", room.breadth.toDouble())
                rObj.put("colorHex", room.colorHex ?: "")

                val doorsArray = JSONArray()
                for (door in room.doors) {
                    val dObj = JSONObject()
                    dObj.put("id", door.id)
                    dObj.put("wall", door.wall.name)
                    dObj.put("offsetMeters", door.offsetMeters.toDouble())
                    dObj.put("widthMeters", door.widthMeters.toDouble())
                    dObj.put("swingsInward", door.swingsInward)
                    doorsArray.put(dObj)
                }
                rObj.put("doors", doorsArray)

                val windowsArray = JSONArray()
                for (win in room.windows) {
                    val wObj = JSONObject()
                    wObj.put("id", win.id)
                    wObj.put("wall", win.wall.name)
                    wObj.put("offsetMeters", win.offsetMeters.toDouble())
                    wObj.put("widthMeters", win.widthMeters.toDouble())
                    windowsArray.put(wObj)
                }
                rObj.put("windows", windowsArray)
                roomsArray.put(rObj)
            }
            fObj.put("rooms", roomsArray)

            val furnArray = JSONArray()
            for (item in floor.furniture) {
                val fnObj = JSONObject()
                fnObj.put("id", item.id)
                fnObj.put("templateId", item.templateId)
                fnObj.put("name", item.name)
                fnObj.put("category", item.category.name)
                fnObj.put("x", item.x.toDouble())
                fnObj.put("y", item.y.toDouble())
                fnObj.put("length", item.length.toDouble())
                fnObj.put("breadth", item.breadth.toDouble())
                fnObj.put("rotationDegrees", item.rotationDegrees.toDouble())
                fnObj.put("isCustom", item.isCustom)
                fnObj.put("customDescription", item.customDescription ?: "")
                furnArray.put(fnObj)
            }
            fObj.put("furniture", furnArray)

            floorsArray.put(fObj)
        }
        root.put("floors", floorsArray)

        return root.toString(2)
    }

    fun fromJson(jsonStr: String): Project {
        val root = JSONObject(jsonStr)
        val id = root.optString("id", System.currentTimeMillis().toString())
        val name = root.optString("name", "Untitled Plan")
        val unitStr = root.optString("unit", MeasurementUnit.METERS.name)
        val unit = try { MeasurementUnit.valueOf(unitStr) } catch (_: Exception) { MeasurementUnit.METERS }
        val plotLength = root.optDouble("plotLength", 15.0).toFloat()
        val plotBreadth = root.optDouble("plotBreadth", 10.0).toFloat()
        val plotWidth1 = root.optDouble("plotWidth1", plotBreadth.toDouble()).toFloat()
        val plotWidth2 = root.optDouble("plotWidth2", plotBreadth.toDouble()).toFloat()
        val plotLength1 = root.optDouble("plotLength1", plotLength.toDouble()).toFloat()
        val plotLength2 = root.optDouble("plotLength2", plotLength.toDouble()).toFloat()
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())
        val isBuiltin = root.optBoolean("isBuiltinTemplate", false)
        val desc = root.optString("description", "")
        val author = root.optString("author", "Local Designer")

        val floors = mutableListOf<Floor>()
        val floorsArray = root.optJSONArray("floors") ?: JSONArray()
        for (i in 0 until floorsArray.length()) {
            val fObj = floorsArray.getJSONObject(i)
            val fId = fObj.optString("id", "floor_$i")
            val fName = fObj.optString("name", if (i == 0) "Ground Floor" else "Floor ${i + 1}")
            val fLevel = fObj.optInt("level", i)
            val fLength = fObj.optDouble("length", plotLength.toDouble()).toFloat()
            val fBreadth = fObj.optDouble("breadth", plotBreadth.toDouble()).toFloat()

            val rooms = mutableListOf<RoomItem>()
            val roomsArray = fObj.optJSONArray("rooms") ?: JSONArray()
            for (j in 0 until roomsArray.length()) {
                val rObj = roomsArray.getJSONObject(j)
                val rId = rObj.optString("id", "room_${i}_$j")
                val rName = rObj.optString("name", "Room")
                val typeStr = rObj.optString("type", RoomType.CUSTOM.name)
                val rType = try { RoomType.valueOf(typeStr) } catch (_: Exception) { RoomType.CUSTOM }
                val rx = rObj.optDouble("x", 0.0).toFloat()
                val ry = rObj.optDouble("y", 0.0).toFloat()
                val rlen = rObj.optDouble("length", 4.0).toFloat()
                val rbr = rObj.optDouble("breadth", 3.0).toFloat()
                val colorHex = rObj.optString("colorHex").takeIf { it.isNotEmpty() }

                val doors = mutableListOf<DoorOpening>()
                val doorsArray = rObj.optJSONArray("doors") ?: JSONArray()
                for (d in 0 until doorsArray.length()) {
                    val dObj = doorsArray.getJSONObject(d)
                    val dId = dObj.optString("id", "door_$d")
                    val wallStr = dObj.optString("wall", WallEdge.BOTTOM.name)
                    val wall = try { WallEdge.valueOf(wallStr) } catch (_: Exception) { WallEdge.BOTTOM }
                    val dOffset = dObj.optDouble("offsetMeters", 0.5).toFloat()
                    val dWidth = dObj.optDouble("widthMeters", 0.9).toFloat()
                    val dSwing = dObj.optBoolean("swingsInward", true)
                    doors.add(DoorOpening(dId, wall, dOffset, dWidth, dSwing))
                }

                val windows = mutableListOf<WindowOpening>()
                val windowsArray = rObj.optJSONArray("windows") ?: JSONArray()
                for (w in 0 until windowsArray.length()) {
                    val wObj = windowsArray.getJSONObject(w)
                    val wId = wObj.optString("id", "window_$w")
                    val wallStr = wObj.optString("wall", WallEdge.TOP.name)
                    val wall = try { WallEdge.valueOf(wallStr) } catch (_: Exception) { WallEdge.TOP }
                    val wOffset = wObj.optDouble("offsetMeters", 0.8).toFloat()
                    val wWidth = wObj.optDouble("widthMeters", 1.2).toFloat()
                    windows.add(WindowOpening(wId, wall, wOffset, wWidth))
                }

                rooms.add(RoomItem(rId, rName, rType, rx, ry, rlen, rbr, doors, windows, colorHex))
            }

            val furniture = mutableListOf<FurnitureItem>()
            val furnArray = fObj.optJSONArray("furniture") ?: JSONArray()
            for (k in 0 until furnArray.length()) {
                val fnObj = furnArray.getJSONObject(k)
                val fnId = fnObj.optString("id", "furn_${i}_$k")
                val fnTemplate = fnObj.optString("templateId", "generic")
                val fnName = fnObj.optString("name", "Furniture")
                val catStr = fnObj.optString("category", FurnitureCategory.CUSTOM.name)
                val cat = try { FurnitureCategory.valueOf(catStr) } catch (_: Exception) { FurnitureCategory.CUSTOM }
                val fx = fnObj.optDouble("x", 0.0).toFloat()
                val fy = fnObj.optDouble("y", 0.0).toFloat()
                val flen = fnObj.optDouble("length", 1.0).toFloat()
                val fbr = fnObj.optDouble("breadth", 1.0).toFloat()
                val fRot = fnObj.optDouble("rotationDegrees", 0.0).toFloat()
                val isCust = fnObj.optBoolean("isCustom", false)
                val custDesc = fnObj.optString("customDescription").takeIf { it.isNotEmpty() }

                furniture.add(FurnitureItem(fnId, fnTemplate, fnName, cat, fx, fy, flen, fbr, fRot, isCust, custDesc))
            }

            floors.add(Floor(fId, fName, fLevel, fLength, fBreadth, rooms, furniture))
        }

        return Project(
            id = id,
            name = name,
            unit = unit,
            plotLength = plotLength,
            plotBreadth = plotBreadth,
            plotWidth1 = plotWidth1,
            plotWidth2 = plotWidth2,
            plotLength1 = plotLength1,
            plotLength2 = plotLength2,
            floors = floors,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isBuiltinTemplate = isBuiltin,
            description = desc,
            author = author
        )
    }
}
