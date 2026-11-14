package com.example.smarty

import com.example.smarty.model.ProductBarcodeParser

class ProductBarcodeParserTest {
    @org.junit.Test
    fun handlesBarcodeAndMetadataEdgeCases() {
    var checks = 0
    fun verify(condition: Boolean) { check(condition); checks++ }
    verify(ProductBarcodeParser.normalize(" 000123456789 ") == "000123456789")
    verify(ProductBarcodeParser.normalize("3017 6204 22003") == "3017620422003")
    for (code in listOf("123", "abcdefgh", "1234567x", "https://example.com", "", "123456789")) {
        verify(runCatching { ProductBarcodeParser.normalize(code) }.isFailure)
    }
    val product = ProductBarcodeParser.parse("3017620422003", """{"status":"success","product":{"product_name_en":"Milk","product_name_fr":"Lait","brands":"Brand","quantity":"500 mL","image_front_small_url":"https://example.com/milk.jpg"}}""")!!
    verify(product.name == "Milk")
    verify(product.barcode == "3017620422003")
    verify(product.packageQuantity == "500 mL")
    verify(product.imageUrl == "https://example.com/milk.jpg")
    verify(ProductBarcodeParser.parse("000123456789", """{"product":{"product_name_en":null,"product_name_fr":"Lait"}}""")!!.name == "Lait")
    verify(ProductBarcodeParser.parse("000123456789", """{"product":{"product_name":"Generic"}}""")!!.name == "Generic")
    verify(ProductBarcodeParser.parse("000123456789", """{"product":{}}""")!!.name == "")
    verify(ProductBarcodeParser.parse("000123456789", """{"status":"failure"}""") == null)
    verify(ProductBarcodeParser.parse("000123456789", """{}""") == null)
    verify(ProductBarcodeParser.parse("000123456789", """{"product":{"image_front_url":"http://example.com/unsafe.jpg"}}""")!!.imageUrl == "")
    verify(runCatching { ProductBarcodeParser.parse("000123456789", "invalid json") }.isFailure)
    check(checks == 19)
}

}
