package com.ghostos.ghost_os.core

data class InstalledApp(val name: String, val packageName: String)

sealed class AppMatch {
    data class Found(val app: InstalledApp) : AppMatch()
    data class Ambiguous(val apps: List<InstalledApp>) : AppMatch()
    data object Missing : AppMatch()
}

/** Prefer exact names; allow whole-word partial names only when unique. */
object AppMatcher {
    private fun normalize(value: String) = value.lowercase(java.util.Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    fun match(query: String, apps: List<InstalledApp>): AppMatch {
        val name = normalize(query)
        if (name.isEmpty()) return AppMatch.Missing
        val exact = apps.filter { normalize(it.name) == name || it.packageName.equals(query.trim(), true) }
        if (exact.isNotEmpty()) return resolve(exact)
        val shortcuts = mapOf("you tube" to "com.google.android.youtube", "whats app" to "com.whatsapp",
            "whatsapp" to "com.whatsapp", "youtube" to "com.google.android.youtube",
            "gmail" to "com.google.android.gm", "chrome" to "com.android.chrome",
            "settings" to "com.android.settings", "instagram" to "com.instagram.android")
        shortcuts[name]?.let { pkg -> apps.firstOrNull { it.packageName == pkg }?.let { return AppMatch.Found(it) } }
        if (name.length < 3) return AppMatch.Missing
        return resolve(apps.filter { (" " + normalize(it.name) + " ").contains(" $name ") })
    }
    private fun resolve(apps: List<InstalledApp>): AppMatch {
        val unique = apps.distinctBy { it.packageName }
        return when (unique.size) { 0 -> AppMatch.Missing; 1 -> AppMatch.Found(unique.first()); else -> AppMatch.Ambiguous(unique) }
    }
}
