package com.example.engine.fs

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader

object TextExtractor {

    suspend fun extractTextFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n")
                }
                Result.success(stringBuilder.toString())
            } ?: Result.failure(Exception("Could not open input stream for URI"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractTextFromFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.isFile) {
                return@withContext Result.failure(Exception("File does not exist or is a directory"))
            }
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
