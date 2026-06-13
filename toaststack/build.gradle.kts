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

android {
    namespace = "com.siliconcircuits.toaststack"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
                version = "1.0.5"

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
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.compose.animation)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
