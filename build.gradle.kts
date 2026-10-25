pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // LiveKit's audio helper (audioswitch) is hosted on JitPack
    maven {
      url = uri("https://jitpack.io")
      content { includeGroupByRegex("com\\.github.*") }
    }
  }
}

rootProject.name = "Checkmate"

include(":app")
