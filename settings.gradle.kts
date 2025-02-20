import java.net.URI

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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = URI("https://jitpack.io") }
                maven {
            url = uri("https://pdftron-maven.s3.amazonaws.com/release")
        }
//        maven {
//            url = uri("https://my.nutrient.io/maven")
//        }
//        maven {
//            url = uri("http://maven.ghostscript.com")
//        }

    }
}

rootProject.name = "MedStandarts_v2"
include(":app")

 