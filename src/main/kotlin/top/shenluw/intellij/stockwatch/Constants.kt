package top.shenluw.intellij.stockwatch

import com.intellij.util.messages.Topic

/**
 * @author Shenluw
 * created: 2020/3/21 21:22
 */
const val WSS_API_ADDRESS = "wss://openapi.itiger.com:8887/stomp"
const val HTTP_API_ADDRESS = "https://openapi.itiger.com/gateway"
//val API_ADDRESS = "wss://openapi-sandbox.itiger.com:8889/stomp"

const val PLUGIN_ID = "top.shenluw.intellij.stock-watch"
const val PLUGIN_NAME = "Stock Watch"
const val TIGER_HELP_LINK = "https://quant.itiger.com/openapi/java-docs/zh-cn/docs/intro/quickstart.html"

val QuotesTopic = Topic.create("Quotes", QuotesService.QuotesListener::class.java)
