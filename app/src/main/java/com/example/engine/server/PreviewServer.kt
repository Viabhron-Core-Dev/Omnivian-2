package com.example.engine.server

import fi.iki.elonen.NanoHTTPD
import android.util.Log
import java.io.File
import java.io.FileInputStream

class PreviewServer(
    port: Int,
    private val workspaceRoot: File
) : NanoHTTPD("127.0.0.1", port) {

    init {
        Log.d("PreviewServer", "Initializing PreviewServer on 127.0.0.1:\$port with root: \${workspaceRoot.absolutePath}")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val path = if (uri == "/") "/index.html" else uri
        
        val targetFile = File(workspaceRoot, path)
        
        return if (targetFile.exists() && targetFile.isFile) {
            val mimeType = getMimeTypeForFile(uri)
            newChunkedResponse(Response.Status.OK, mimeType, FileInputStream(targetFile))
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File Not Found")
        }
    }
}
