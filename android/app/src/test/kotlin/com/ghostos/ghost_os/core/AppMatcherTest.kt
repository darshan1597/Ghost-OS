package com.ghostos.ghost_os.core

import org.junit.Assert.*
import org.junit.Test

class AppMatcherTest {
    private val apps = listOf(InstalledApp("YouTube", "com.google.android.youtube"),
        InstalledApp("YouTube Music", "com.google.android.apps.youtube.music"),
        InstalledApp("Vivo Notes", "com.vivo.notes"), InstalledApp("WhatsApp", "com.whatsapp"))
    @Test fun exactNameBeatsPartialName() {
        assertEquals(AppMatch.Found(apps[0]), AppMatcher.match("youtube", apps))
    }
    @Test fun knownSpeechVariationAndUniqueWordMatch() {
        assertEquals(AppMatch.Found(apps[0]), AppMatcher.match("you tube", apps))
        assertEquals(AppMatch.Found(apps[2]), AppMatcher.match("notes", apps))
    }
    @Test fun ambiguityIsNotGuessed() {
        assertTrue(AppMatcher.match("notes", apps + InstalledApp("Other Notes", "other.notes")) is AppMatch.Ambiguous)
        assertTrue(AppMatcher.match("maps", apps) is AppMatch.Missing)
        assertTrue(AppMatcher.match("app", apps) is AppMatch.Missing)
    }
    @Test fun duplicateActivitiesDoNotCreateFalseAmbiguity() {
        assertEquals(AppMatch.Found(apps[3]), AppMatcher.match("whatsapp", apps + apps[3]))
        assertEquals(AppMatch.Found(apps[2]), AppMatcher.match("com.vivo.notes", apps))
    }
}
