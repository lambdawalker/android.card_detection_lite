package com.apexfission.android.carddetectionlite.tfmodel

import com.apexfission.android.carddetectionlite.domain.ModelCatalog


val ModelCatalog.TfLite.Companion.modelPath: String
    get() = "cdl/tflite/E83Y11_640B1F16.tflite"

val ModelCatalog.TfLite.Companion.modelName: String
    get() = "E83Y11_640B1F16.tflite"

val ModelCatalog.TfLite.Companion.classes: Map<Int, String>
    get() = mapOf(0 to "HFront", 1 to "Back", 2 to "VFront")

