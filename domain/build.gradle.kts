// Ruta: domain/build.gradle.kts (REEMPLAZA el contenido anterior - se agregó kotlinx-coroutines-core)
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.androidx.compose.runtime)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(libs.junit)
    implementation(kotlin("test"))


}