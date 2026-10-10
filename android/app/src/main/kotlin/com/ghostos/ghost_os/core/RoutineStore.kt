package com.ghostos.ghost_os.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class GhostRoutine(val name: String, val steps: List<String>)

/** Small, local routine store. Only commands accepted by [safeStep] can be saved. */
class RoutineStore(context: Context) {
    private val prefs = context.getSharedPreferences("ghost_routines", Context.MODE_PRIVATE)

    fun all(): List<GhostRoutine> = runCatching {
        val array = JSONArray(prefs.getString("items", "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val rawSteps = item.getJSONArray("steps")
            GhostRoutine(item.getString("name"), (0 until rawSteps.length()).map(rawSteps::getString))
        }
    }.getOrDefault(emptyList())

    fun save(name: String, steps: List<String>): GhostRoutine {
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        require(cleanName.length in 2..32) { "Use a routine name between 2 and 32 characters." }
        require(steps.size in 1..6) { "A routine needs between 1 and 6 steps." }
        require(steps.all { safeStep(CommandParser.parse(it)) }) {
            "Routines currently support opening apps, media controls, and driving mode only."
        }
        val routine = GhostRoutine(cleanName, steps.map { it.trim() })
        val items = all().filterNot { it.name.equals(cleanName, true) } + routine
        write(items.sortedBy { it.name.lowercase() })
        return routine
    }

    fun find(name: String): GhostRoutine? = all().firstOrNull { it.name.equals(name.trim(), true) }
        ?: all().filter { it.name.contains(name.trim(), true) }.singleOrNull()

    fun delete(name: String): Boolean {
        val found = find(name) ?: return false
        write(all().filterNot { it.name.equals(found.name, true) })
        return true
    }

    private fun write(items: List<GhostRoutine>) {
        val array = JSONArray()
        items.forEach { routine ->
            array.put(JSONObject().put("name", routine.name).put("steps", JSONArray(routine.steps)))
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    companion object {
        fun safeStep(command: GhostCommand) = command is GhostCommand.OpenApp ||
            command is GhostCommand.Media || command is GhostCommand.DrivingMode
    }
}
