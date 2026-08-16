package com.example.engine.fs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object GithubDownloader {
    
    // Simplistic downloader that attempts to download the zip of the 'main' branch
    suspend fun downloadRepoAsZip(repoUrl: String, destZipFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Convert https://github.com/user/repo to https://github.com/user/repo/archive/refs/heads/main.zip
            val cleanUrl = repoUrl.trim().removeSuffix("/")
            val zipUrl = "$cleanUrl/archive/refs/heads/main.zip"
            
            val url = URL(zipUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    BufferedOutputStream(FileOutputStream(destZipFile)).use { output ->
                        val data = ByteArray(1024)
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            output.write(data, 0, count)
                        }
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to download repo: HTTP ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
