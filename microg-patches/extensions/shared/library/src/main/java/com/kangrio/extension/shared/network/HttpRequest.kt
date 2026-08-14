package com.kangrio.extension.shared.network

import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.Executors

object HttpRequest {

    fun get(url: String): String {
        return Executors.newSingleThreadExecutor().submit<String> {
            val connection = (java.net.URL(url).openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.useCaches = false

                val responseCode = connection.responseCode

                if (responseCode !in 200..299) {
                    throw IOException("HTTP $responseCode")
                }

                connection.inputStream.bufferedReader().use {
                    it.readText()
                }
            } finally {
                connection.disconnect()
            }
        }.get()
    }
}