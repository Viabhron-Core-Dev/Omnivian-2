package com.example.ui.code

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PdfViewer(file: File, modifier: Modifier = Modifier) {
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(file) {
        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            fileDescriptor = fd
            pdfRenderer = renderer
        } catch (e: Exception) {
            error = e.message
        }
        
        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        pdfRenderer?.let { renderer ->
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(renderer.pageCount) { index ->
                    PdfPageViewer(renderer = renderer, pageIndex = index)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        } ?: error?.let { e ->
            Text(text = "Error loading PDF: $e", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        } ?: run {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun PdfPageViewer(renderer: PdfRenderer, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(renderer, pageIndex) {
        try {
            val page = renderer.openPage(pageIndex)
            val w = page.width * 2
            val h = page.height * 2
            val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            // Render on white background
            bm.eraseColor(android.graphics.Color.WHITE)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap = bm
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    bitmap?.let { b ->
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = "PDF Page ${pageIndex + 1}",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    } ?: run {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
