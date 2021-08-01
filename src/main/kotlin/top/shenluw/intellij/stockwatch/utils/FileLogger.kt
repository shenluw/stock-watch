package top.shenluw.intellij.stockwatch.utils

import com.intellij.openapi.diagnostic.Logger
import java.io.Closeable
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.logging.Level

/**
 * @author Shenluw
 * created: 2020/5/4 19:10
 */
class FileLogger(val path: String, val logger: Logger) : Closeable, AutoCloseable {

    var writeable = true

    fun warn(message: String?, t: Throwable?) {
        logger.warn(message, t)
        write(Level.WARNING, message, t)
    }

    fun warn(message: String?, vararg details: String?) {
        logger.warn("$message ${details.contentToString()}")
        write(Level.WARNING, message, null, *details)
    }

    fun info(message: String?) {
        logger.info(message)
        write(Level.INFO, message)
    }

    fun info(message: String?, t: Throwable?) {
        logger.info(message, t)
        write(Level.INFO, message, t)
    }

    fun info(message: String?, vararg details: String?) {
        logger.info("$message ${details.contentToString()}")
        write(Level.INFO, message, null, *details)
    }

    fun error(message: String?, t: Throwable?, vararg details: String?) {
        logger.error(message, t, *details)
        write(Level.WARNING, message, t, *details)
    }

    fun isDebugEnabled(): Boolean {
        return logger.isDebugEnabled
    }

    fun debug(message: String?) {
        logger.debug(message)
        write(Level.WARNING, message)
    }

    fun debug(t: Throwable?) {
        logger.debug(t)
        write(Level.WARNING, null, t)
    }

    fun debug(message: String?, t: Throwable?) {
        logger.debug(message, t)
        write(Level.WARNING, message, t)
    }

    fun debug(message: String?, vararg details: String?) {
        logger.debug("$message ${details.contentToString()}")
        write(Level.WARNING, message, null, *details)
    }

    private fun write(level: Level, message: String?, t: Throwable? = null, vararg details: String?) {
        if (!writeable) {
            return
        }
        if (writer == null) {
            open()
        }
        if (message != null) {
            val d: String = if (details.isNotEmpty()) {
                details.contentToString()
            } else {
                ""
            }
            writer?.println("$level: $message $d")
        }
        t?.printStackTrace(writer)
        writer?.flush()
    }

    private var writer: PrintWriter? = null

    @Synchronized
    fun open() {
        if (writer == null) {
            writer = PrintWriter(FileWriter(File(path), true))
        }
    }

    override fun close() {
        try {
            writer?.close()
            writer = null
        } catch (e: Exception) {
            logger.error("close write error", e)
        }
    }
}