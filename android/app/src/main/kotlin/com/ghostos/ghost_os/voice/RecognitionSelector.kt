package com.ghostos.ghost_os.voice

import com.ghostos.ghost_os.core.CommandParser
import com.ghostos.ghost_os.core.GhostCommand
import org.json.JSONObject

data class RecognitionCandidate(val text: String, val confidence: Double)
data class RecognitionDecision(
    val text: String,
    val confidence: Double,
    val alternative: String? = null,
    val understoodAction: Boolean = false,
) {
    val uncertain get() = confidence in 0.01..0.57 && !understoodAction
}

/** Selects the most useful offline hypothesis without changing or inventing its words. */
object RecognitionSelector {
    fun candidates(json: String): List<RecognitionCandidate> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val alternatives = root.optJSONArray("alternatives")
        if (alternatives != null) return buildList {
            for (index in 0 until alternatives.length()) {
                val item = alternatives.optJSONObject(index) ?: continue
                val text = item.optString("text").trim()
                if (text.isNotEmpty()) add(RecognitionCandidate(text, item.optDouble("confidence", 0.0)))
            }
        }.distinctBy { it.text }
        val text = root.optString("text").trim()
        return if (text.isEmpty()) emptyList() else listOf(RecognitionCandidate(text, 0.0))
    }

    fun select(json: String, commandMode: Boolean): String {
        return decide(candidates(json), commandMode).text
    }

    fun select(values: List<RecognitionCandidate>, commandMode: Boolean): String {
        return decide(values, commandMode).text
    }

    fun decide(json: String, commandMode: Boolean) = decide(candidates(json), commandMode)

    fun decide(values: List<RecognitionCandidate>, commandMode: Boolean): RecognitionDecision {
        if (values.isEmpty()) return ""
            .let { RecognitionDecision(it, 0.0) }
        val ranked = if (!commandMode) values.sortedByDescending { it.confidence }
            else values.sortedWith(compareByDescending<RecognitionCandidate> { semanticScore(it.text) }.thenByDescending { it.confidence })
        val selected = ranked.first()
        val alternative = ranked.drop(1).firstOrNull { it.text != selected.text && semanticScore(it.text) >= semanticScore(selected.text) }?.text
        return RecognitionDecision(selected.text, selected.confidence, alternative, semanticScore(selected.text) > 0)
    }

    internal fun semanticScore(text: String): Int = when (CommandParser.parse(text)) {
        is GhostCommand.Unknown -> 0
        is GhostCommand.Chat -> 1
        else -> 5
    }
}
