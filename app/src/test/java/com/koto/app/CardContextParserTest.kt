package com.koto.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CardContextParserTest {
    @Test
    fun verifyCardContextsAssetExistsAndIsPopulated() {
        val rootDir = File(".").canonicalFile
        val assetsDirCandidates = listOf(
            File(rootDir, "src/main/assets"),
            File(rootDir, "app/src/main/assets"),
            File("c:/Users/HMED OPEN/Documents/koto/koto the project/app/src/main/assets"),
        )
        val assetsDir = assetsDirCandidates.firstOrNull { it.exists() }
        assertNotNull("assets directory must exist", assetsDir)

        val jsonFile = File(assetsDir, "card_contexts.json")
        assertTrue("card_contexts.json must exist in assets", jsonFile.exists())
        assertTrue("card_contexts.json must have substantial size", jsonFile.length() > 500000L)
    }
}
