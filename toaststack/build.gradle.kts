plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

// Pin the JDK used to build, test, and generate docs for this repo to 21
// (matching CI), regardless of the developer's default JDK. This is scoped to
// this project only - it does not change any machine-global Gradle setting.
// JDK 25/26 break Robolectric (ASM cannot read the bytecode) and Dokka
// (cannot parse the version string), so a toolchain keeps local builds aligned
// with CI. Gradle auto-provisions JDK 21 if it is not already installed.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Kotlin 2.0 metadata and stdlib, so apps on Kotlin 2.0 can read the library.
kotlin {
    coreLibrariesVersion = "2.0.21"
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

android {
    namespace = "com.siliconcircuits.toaststack"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")

        // The floor for apps that use the library. It matches the highest
        // floor of the runtime dependencies and README "Requirements".
        aarMetadata {
            minCompileSdk = 35
            minAgpVersion = "8.6.0"
        }
    }

    // Java 11 bytecode, so apps on older toolchains can use the library.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Plain JVM tests reach android.util.Log when a toast is dropped.
            isReturnDefaultValues = true
            all { test ->
                // PublicApiTest reads these, so a change to either reruns the tests.
                test.inputs.file(rootProject.file("README.md"))
                test.inputs.file(file("api/toaststack.api"))
                // ./gradlew :toaststack:testDebugUnitTest -PupdateApiDump writes a new dump.
                test.systemProperty("updateApiDump", project.hasProperty("updateApiDump"))
                // -PrecordScreenshots writes the README images to docs/images.
                test.systemProperty("recordScreenshots", project.hasProperty("recordScreenshots"))
                // Hardware rendering draws card shadows in the README images.
                if (project.hasProperty("recordScreenshots")) {
                    test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                }
            }
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.siliconcircuits"
                artifactId = "toaststack"
                version = "1.3.0"

                pom {
                    name.set("ToastStack")
                    description.set(
                        "A modern, Compose native toast and notification library for Android. " +
                        "No Scaffold required. One liner API. Stackable. Themeable. Animated."
                    )
                    url.set("https://github.com/zeevy/ToastStack")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("zeevy")
                            name.set("zeevy")
                            url.set("https://github.com/zeevy")
                        }
                    }

                    scm {
                        url.set("https://github.com/zeevy/ToastStack")
                        connection.set("scm:git:git://github.com/zeevy/ToastStack.git")
                        developerConnection.set("scm:git:ssh://github.com/zeevy/ToastStack.git")
                    }
                }
            }
        }
    }
}

dependencies {
    // Types from these appear in the public API (Composable, Modifier, Color,
    // Shape, RoundedCornerShape, TextStyle, PaddingValues, Easing, ViewModel), so apps get them too.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.ui)
    api(libs.androidx.foundation)
    api(libs.androidx.foundation.layout)
    api(libs.androidx.compose.animation.core)
    api(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.compose.animation)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.binary.compatibility.validator)
    testImplementation(libs.kotlin.metadata.jvm)
    testImplementation(libs.asm.tree)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
