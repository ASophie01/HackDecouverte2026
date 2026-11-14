package com.example.smarty.model

import org.json.JSONObject

/** Product metadata is a suggestion; stock amount and expiry date remain user-entered. */
data class ScannedProduct(
    val barcode: String,
    val name: String = "",
    val imageUrl: String = "",
    val brand: String = "",
    val packageQuantity: String = ""
)

object ProductBarcodeParser {
    fun normalize(raw: String): String {
        val code = raw.filterNot { it.isWhitespace() }
        require(code.length in listOf(8, 12, 13, 14) && code.all { it in '0'..'9' }) {
            "Enter a grocery barcode with 8, 12, 13 or 14 digits."
        }
        // Never convert to a number: leading zeroes are significant.
        return code
    }

    fun parse(rawBarcode: String, response: String): ScannedProduct? {
        val code = normalize(rawBarcode)
        val json = JSONObject(response)
        if (json.optString("status") == "failure") return null
        val product = json.optJSONObject("product") ?: return null

        fun field(name: String): String {
            return if (product.isNull(name)) "" else product.optString(name).trim()
        }

        val name = listOf("product_name_en", "product_name_fr", "product_name", "generic_name")
            .firstNotNullOfOrNull { field(it).takeIf(String::isNotBlank) }.orEmpty()
        val image = field("image_front_small_url").ifBlank { field("image_front_url") }

        return ScannedProduct(
            barcode = code,
            name = name.take(200),
            imageUrl = image.takeIf { it.startsWith("https://") }.orEmpty(),
            brand = field("brands").take(120),
            packageQuantity = field("quantity").take(120)
        )
    }
}
