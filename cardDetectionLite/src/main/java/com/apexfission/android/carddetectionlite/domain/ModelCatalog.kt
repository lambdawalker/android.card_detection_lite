package com.apexfission.android.carddetectionlite.domain

/**
 * Provides a centralized catalog for accessing details of different machine learning models.
 *
 * This class acts as a namespace to organize model-specific information. It is designed
 * to be extended by other modules that provide concrete model implementations.
 *
 * For example, an imported module extends the nested `TfLite.Companion` object
 * to provide details about a specific TensorFlow Lite model, such as its asset path,
 * class mappings, and other relevant metadata. This decouples the core logic from the
 * specific model details, allowing for easier model swapping and management.
 *
 */
class ModelCatalog {
    /**
     * A nested class that serves as a namespace for TensorFlow Lite model properties.
     *
     * Its `Companion` object is the target for extension functions and properties that
     * define the specifics of a given TFLite model.
     */
    class TfLite {
        /**
         * The companion object that is extended by other modules to provide
         * concrete model details.
         */
        companion object
    }
}