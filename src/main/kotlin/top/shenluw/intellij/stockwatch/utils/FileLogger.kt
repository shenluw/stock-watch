package top.shenluw.intellij.stockwatch.utils

import com.intellij.openapi.diagnostic.Logger
import org.apache.log4j.Level
import java.io.Closeable
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

/**
 * @author Shenluw
 * created: 2020/5/4 19:10
 */
class FileLogger(val path: String, val logger: Logger) : Logger(), Closeable, AutoCloseable {

    var writeable = true

    override fun setLevel(level: Level?) {
        logger.setLevel(level)
    }

    override fun warn(message: String?, t: Throwable?) {
        logger.warn(message, t)
        write(Level.WARN, message, t)
    }

    fun warn(message: String?, vararg details: String?) {
        logger.warn("$message ${details.contentToString()}")
        write(Level.WARN, message, null, *details)
    }

    override fun info(message: String?) {
        logger.info(message)
        write(Level.INFO, message)
    }

    override fun info(message: String?, t: Throwable?) {
        logger.info(message, t)
        write(Level.INFO, message, t)
    }

    fun info(message: String?, vararg details: String?) {
        logger.info("$message ${details.contentToString()}")
        write(Level.INFO, message, null, *details)
    }

    override fun error(message: String?, t: Throwable?, vararg details: String?) {
        logger.error(message, t, *details)
        write(Level.ERROR, message, t, *details)
    }

    override fun isDebugEnabled(): Boolean {
        return logger.isDebugEnabled
    }

    override fun debug(message: String?) {
        logger.debug(message)
        write(Level.DEBUG, message)
    }

    override fun debug(t: Throwable?) {
        logger.debug(t)
        write(Level.DEBUG, null, t)
    }

    override fun debug(message: String?, t: Throwable?) {
        logger.debug(message, t)
        write(Level.DEBUG, message, t)
    }

    fun debug(message: String?, vararg details: String?) {
        logger.debug("$message ${details.contentToString()}")
        write(Level.DEBUG, message, null, *details)
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
            writer?.write("$level: $message $d")
        }
        t?.printStackTrace(writer)
        writer?.flush()
    }

    private var writer: PrintWriter? = null

    @Synchronized
    fun open() {
        if (writer == null) {
            writer = PrintWriter(FileWriter(File(path), StandardCharsets.UTF_8, true))
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