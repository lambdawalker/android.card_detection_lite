package com.apexfission.android.carddetectionlite

// This tells Kotlin the annotation can be applied to functions (methods) or entire classes
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
// This ensures the annotation is readable by the test runner when the test executes
@Retention(AnnotationRetention.RUNTIME)
annotation class GenerateImage