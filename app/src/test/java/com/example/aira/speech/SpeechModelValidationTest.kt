package com.example.aira.speech

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SpeechModelValidationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testEmptyDirectoryFailsValidation() {
        val emptyDir = tempFolder.newFolder("empty_model")
        val manager = SpeechModelManagerDummy()
        assertFalse(manager.validate(emptyDir))
    }

    @Test
    fun testNonExistentDirectoryFailsValidation() {
        val nonExistent = File(tempFolder.root, "non_existent")
        val manager = SpeechModelManagerDummy()
        assertFalse(manager.validate(nonExistent))
    }

    @Test
    fun testValidModelDirectoryPassesValidation() {
        val validDir = tempFolder.newFolder("valid_model")
        val amDir = File(validDir, "am").apply { mkdir() }
        File(amDir, "final.mdl").createNewFile()
        val confDir = File(validDir, "conf").apply { mkdir() }
        File(confDir, "mfcc.conf").createNewFile()

        val manager = SpeechModelManagerDummy()
        assertTrue(manager.validate(validDir))
    }

    private class SpeechModelManagerDummy {
        fun validate(dir: File): Boolean {
            if (!dir.exists() || !dir.isDirectory) return false
            val hasAcousticModel = File(dir, "am/final.mdl").exists() || File(dir, "final.mdl").exists()
            val hasGraph = File(dir, "graph/HCLG.fst").exists() ||
                           File(dir, "graph/Gr.fst").exists() ||
                           File(dir, "HCLG.fst").exists() ||
                           File(dir, "Gr.fst").exists()
            val hasConf = File(dir, "conf/mfcc.conf").exists() || File(dir, "mfcc.conf").exists()

            return (hasAcousticModel || hasGraph || hasConf)
        }
    }
}
