package com.example.engine.fs

import java.io.File

data class FileNode(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val children: List<FileNode> = emptyList(),
    val size: Long = file.length(),
    val lastModified: Long = file.lastModified()
)
