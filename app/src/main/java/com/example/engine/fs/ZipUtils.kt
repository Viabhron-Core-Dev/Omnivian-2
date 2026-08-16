package com.example.engine.fs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    suspend fun unzip(zipFile: File, targetDirectory: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!targetDirectory.exists()) {
                targetDirectory.mkdirs()
            }
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDirectory, entry.name)
                    // Security check to prevent Zip Slip vulnerability
                    val canonicalDestPath = targetDirectory.canonicalPath
                    val canonicalNewFilePath = newFile.canonicalPath
                    if (!canonicalNewFilePath.startsWith(canonicalDestPath + File.separator)) {
                         return@withContext Result.failure(Exception("Entry is outside of the target dir: ${entry.name}"))
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(newFile)).use { bos ->
                            val buffer = ByteArray(1024)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                bos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun zipDirectory(sourceDirectory: File, targetZipFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            targetZipFile.parentFile?.mkdirs()
            ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
                zipFile(sourceDirectory, sourceDirectory, zos)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun zipFile(fileToZip: File, rootDir: File, zos: ZipOutputStream) {
        if (fileToZip.isHidden) return

        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles()
            if (children != null) {
                for (childFile in children) {
                    zipFile(childFile, rootDir, zos)
                }
            }
        } else {
            val entryName = fileToZip.absolutePath.substring(rootDir.absolutePath.length + 1)
            val zipEntry = ZipEntry(entryName)
            zos.putNextEntry(zipEntry)
            FileInputStream(fileToZip).use { fis ->
                val buffer = ByteArray(1024)
                var length: Int
                while (fis.read(buffer).also { length = it } >= 0) {
                    zos.write(buffer, 0, length)
                }
            }
            zos.closeEntry()
        }
    }
}
