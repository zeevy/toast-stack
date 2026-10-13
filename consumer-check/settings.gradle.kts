// A small app on the oldest setup the library supports (README "Requirements").
// CI builds it against the library published to Maven Local, so a change that
// raises the floor fails here. It is a separate build, not part of the root one.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal {
            content { includeGroup("com.siliconcircuits") }
        }
        google()
        // The released copy on Maven Central must not stand in for this build.
        mavenCentral {
            content { excludeGroup("com.siliconcircuits") }
        }
    }
}

rootProject.name = "consumer-check"
