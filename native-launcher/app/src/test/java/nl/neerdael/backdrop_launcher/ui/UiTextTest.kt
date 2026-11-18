package nl.neerdael.backdrop_launcher.ui

import org.junit.Assert.*
import org.junit.Test
import nl.neerdael.backdrop_launcher.R

class UiTextTest {
    private val tr = mapOf("__locale" to "tr-TR", "Interface" to "Arayüz",
        "Reorder in %1\$s" to "%1\$s sırasında yeniden sırala", "%1\$d minutes" to "%1\$d dakika",
        "Delete %1\$s?" to "%1\$s silinsin mi?")

    @Test fun templatesAreTranslatedWithoutChangingUserContent() {
        assertEquals("Arayüz", uiText("Interface", tr))
        assertEquals("Filmler sırasında yeniden sırala", uiText("Reorder in Filmler", tr))
        assertEquals("10 dakika", uiText("10 minutes", tr))
        assertEquals("Eğlence silinsin mi?", uiText("Delete Eğlence?", tr))
        assertEquals("YouTube", uiText("YouTube", tr))
        assertEquals("com.netflix.ninja", uiText("com.netflix.ninja", tr))
        assertEquals("Interface", uiText("Interface", emptyMap()))
    }

    @Test fun localeCandidatesSupportRegionsScriptsAndFutureLanguages() {
        assertEquals(listOf("es_MX", "es", "en"), languageCandidates("es-MX"))
        assertEquals(listOf("zh_Hant_TW", "zh", "en"), languageCandidates("zh-Hant-TW"))
        assertEquals(listOf("fr_CA", "fr", "en"), languageCandidates("fr_CA"))
        assertEquals(listOf("tr", "en"), languageCandidates("tr"))
        assertTrue(languageCandidates("../../tr").none { '/' in it || '.' in it })
    }

    @Test fun onlyGeneratedDefaultRowNamesAreTranslated() {
        assertEquals(R.string.vortex_tv_apps, defaultRowLabelRes("TV Apps", 1))
        assertEquals(R.string.vortex_android_apps, defaultRowLabelRes("Android Apps", 2))
        assertNull(defaultRowLabelRes("TV Apps", 0))
        assertNull(defaultRowLabelRes("My shows", 1))
    }
}
