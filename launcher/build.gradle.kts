plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
    // Screenshot tests of the ribbon home (Robolectric renders Compose off-device).
    id("io.github.takahirom.roborazzi") version "1.32.2" apply false
}
