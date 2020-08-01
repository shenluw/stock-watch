package top.shenluw.intellij.stockwatch

import com.intellij.util.messages.Topic

/**
 * @author Shenluw
 * created: 2020/3/21 21:22
 */
const val PLUGIN_ID = "top.shenluw.intellij.stock-watch"
const val PLUGIN_NAME = "Stock Watch"

val QuotesTopic = Topic.create("Quotes", QuotesService.QuotesListener::class.java)
