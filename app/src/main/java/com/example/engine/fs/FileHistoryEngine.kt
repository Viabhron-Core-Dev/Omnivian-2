package com.example.engine.fs

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileHistoryEngine {
    private fun getHistoryDir(file: File): File {
        val historyDir = File(file.parentFile, ".history_${file.name}")
        if (!historyDir.exists()) {
            historyDir.mkdirs()
        }
        return historyDir
    }

    fun saveRevision(file: File) {
        if (!file.exists()) return
        val dir = getHistoryDir(file)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val revisionFile = File(dir, "${timestamp}.txt")
        file.copyTo(revisionFile, overwrite = true)
    }

    fun getRevisions(file: File): List<File> {
        val dir = getHistoryDir(file)
        return dir.listFiles()?.toList()?.sortedByDescending { it.name } ?: emptyList()
    }
    
    fun revertToFile(originalFile: File, revisionFile: File) {
        if (revisionFile.exists()) {
            saveRevision(originalFile) // Save current before revert
            revisionFile.copyTo(originalFile, overwrite = true)
        }
    }
}
