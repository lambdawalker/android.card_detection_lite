package com.apexfission.android.carddetectionlite.tfmodel

import com.apexfission.android.carddetectionlite.domain.ModelCatalog



val ModelCatalog.TfLite.Companion.modelPath: String
//    get() = "cdl/tflite/Y11-640E124F16.tflite"
    get() = "cdl/tflite/Y11-640E197F16.tflite"

val ModelCatalog.TfLite.Companion.modelName: String
//    get() = "Y11-640E124F16.tflite"
    get() = "Y11-640E197F16.tflite"

val ModelCatalog.TfLite.Companion.classes: Map<Int, String>
    get() = mapOf(
        0 to "horizontal_card",
        1 to "vertical_card",
        2 to "horizontal_card_back",
        3 to "photo",
        4 to "slim-barcode",
        5 to "pdf417",
        6 to "mrz-text",
        7 to "barcode",
        8 to "qrcode"
    )

val ModelCatalog.TfLite.Companion.cardClasses: List<Int>
    get() = listOf(0, 1, 2)


