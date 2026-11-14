package com.example.smarty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.smarty.model.ProductBarcodeParser
import com.example.smarty.model.ScannedProduct
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun BarcodeLookupControls(onProduct: (ScannedProduct) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attached by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        attached = true
        onDispose { attached = false }
    }

    fun lookup(raw: String) {
        val normalized = try {
            ProductBarcodeParser.normalize(raw)
        } catch (exception: IllegalArgumentException) {
            error = exception.message
            return
        }
        code = normalized
        busy = true
        error = null
        scope.launch {
            try {
                val product = OpenFoodFactsService.lookup(normalized)
                if (product == null) {
                    error = "Product not found. Use the barcode manually, or add an item without it."
                } else {
                    showDialog = false
                    onProduct(product)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                error = exception.localizedMessage ?: "Lookup failed. You can enter the product manually."
            } finally {
                busy = false
            }
        }
    }

    Button(onClick = { code = ""; error = null; showDialog = true }) {
        Text("Scan / barcode")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy && !scanning) showDialog = false },
            title = { Text("Add by barcode") },
            text = {
                Column {
                    Text("Scan a grocery barcode or enter its digits. The lookup sends only the barcode to Open Food Facts.")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Barcode") },
                        singleLine = true,
                        enabled = !busy && !scanning,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        enabled = !busy && !scanning,
                        onClick = {
                            scanning = true
                            error = null
                            val options = GmsBarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                                .enableAutoZoom()
                                .build()
                            try {
                                GmsBarcodeScanning.getClient(context, options).startScan()
                                    .addOnSuccessListener { result ->
                                        if (attached) {
                                            scanning = false
                                            lookup(result.rawValue.orEmpty())
                                        }
                                    }
                                    .addOnCanceledListener {
                                        if (attached) scanning = false
                                    }
                                    .addOnFailureListener { exception ->
                                        if (attached) {
                                            scanning = false
                                            error = "Scanner unavailable: ${exception.localizedMessage}. You can type the barcode instead."
                                        }
                                    }
                            } catch (exception: Exception) {
                                scanning = false
                                error = exception.localizedMessage ?: "Scanner unavailable. Type the barcode instead."
                            }
                        }
                    ) { Text(if (scanning) "Opening scanner..." else "Scan with camera") }
                    if (busy) {
                        CircularProgressIndicator()
                        Text("Looking up product...")
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(
                        enabled = !busy && !scanning && runCatching { ProductBarcodeParser.normalize(code) }.isSuccess,
                        onClick = {
                            showDialog = false
                            onProduct(ScannedProduct(ProductBarcodeParser.normalize(code)))
                        }
                    ) { Text("Use barcode manually") }
                    Text("Product data: Open Food Facts (ODbL). Confirm the product before saving.")
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && !scanning && code.isNotBlank(), onClick = { lookup(code) }) { Text("Look up") }
            },
            dismissButton = {
                TextButton(enabled = !busy && !scanning, onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
