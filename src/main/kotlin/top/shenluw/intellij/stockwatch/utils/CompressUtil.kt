package top.shenluw.intellij.stockwatch.utils

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.Deflater

/**
 * @author shenlw
 * @date 2020/3/27 16:58
 */

private val gzipParameters = GzipParameters().apply {
    this.compressionLevel = Deflater.BEST_COMPRESSION
}

fun uncompress(text: String): String {
    val stream = ByteArrayInputStream(Base64.getDecoder().decode(text))
    return GzipCompressorInputStream(stream).use {
        return@use String(it.readBytes())
    }
}

fun compress(bytes: ByteArray): String {
    val outputStream = ByteArrayOutputStream()
    GzipCompressorOutputStream(outputStream, gzipParameters).use {
        it.write(bytes)
    }
    return Base64.getEncoder().encodeToString(outputStream.toByteArray())
}