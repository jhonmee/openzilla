// Top-level build file. No plugins are applied here; each module applies what it needs.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Desde Kotlin 2.0 el compilador de Compose viaja con Kotlin y se activa con este plugin,
    // en vez de fijar a mano una kotlinCompilerExtensionVersion.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}
