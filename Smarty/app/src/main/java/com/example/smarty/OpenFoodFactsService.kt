package com.example.smarty

import com.example.smarty.model.ProductBarcodeParser
import com.example.smarty.model.ScannedProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object OpenFoodFactsService {
    private data class CacheEntry(val product: ScannedProduct?, val expiresAt: Long)
    private val cache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()

    suspend fun lookup(rawBarcode: String): ScannedProduct? = withContext(Dispatchers.IO) {
        val code = ProductBarcodeParser.normalize(rawBarcode)
        cache[code]?.let { cached ->
            if (cached.expiresAt > System.currentTimeMillis()) return@withContext cached.product
        }
        val fields = "product_name,product_name_en,product_name_fr,generic_name,brands,quantity,image_front_small_url,image_front_url"
        val connection = URL("https://world.openfoodfacts.org/api/v3/product/$code?fields=$fields")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "Smarty/1.0 (https://github.com/ASophie01/HackDecouverte2026)"
            )
            val product = when (val status = connection.responseCode) {
                404 -> null
                200 -> {
                    val response = connection.inputStream.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            if (output.size() + count > 1024 * 1024) {
                                throw IOException("Product response was too large.")
                            }
                            output.write(buffer, 0, count)
                        }
                        output.toString("UTF-8")
                    }
                    ProductBarcodeParser.parse(code, response)
                }
                429 -> throw IOException("Product service is busy. Try later or add the item manually.")
                else -> throw IOException("Product lookup failed (HTTP $status). You can add it manually.")
            }
            if (cache.size >= 100) cache.clear()
            cache[code] = CacheEntry(product, System.currentTimeMillis() + 10 * 60 * 1000)
            product
        } finally {
            connection.disconnect()
        }
    }
}
