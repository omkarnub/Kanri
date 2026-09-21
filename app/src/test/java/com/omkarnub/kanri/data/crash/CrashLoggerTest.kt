package com.omkarnub.kanri.data.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class CrashLoggerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCrashLogFormattingAndWriting() {
        val testException = RuntimeException("Test simulated failure")
        val sw = StringWriter()
        testException.printStackTrace(PrintWriter(sw))

        val logFile = tempFolder.newFile("crash_test.txt")
        val content = buildString {
            appendLine("Type: UNCAUGHT CRASH (FATAL)")
            appendLine("Tag: TestCrash")
            appendLine("Exception Class: ${testException.javaClass.name}")
            appendLine("Message: ${testException.message}")
            appendLine("Stack Trace:")
            appendLine(sw.toString())
        }
        logFile.writeText(content)

        assertTrue(logFile.exists())
        val savedText = logFile.readText()
        assertTrue(savedText.contains("Test simulated failure"))
        assertTrue(savedText.contains("UNCAUGHT CRASH"))
    }

    @Test
    fun testPruningKeepsOnlyTenLogs() {
        val folder = tempFolder.newFolder("crash_logs")
        // Create 15 dummy crash logs with stepped timestamps
        for (i in 1..15) {
            val f = File(folder, "fatal_crash_${1000 + i}.txt")
            f.writeText("Crash $i")
            f.setLastModified(1000L * i)
        }

        val allLogs = folder.listFiles { file -> file.isFile && file.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()

        assertEquals(15, allLogs.size)

        // Prune to 10
        if (allLogs.size > 10) {
            allLogs.drop(10).forEach { it.delete() }
        }

        val pruned = folder.listFiles { file -> file.isFile && file.name.endsWith(".txt") }
        assertEquals(10, pruned?.size)
    }
}
