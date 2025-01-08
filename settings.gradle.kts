pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/releases") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/releases") }
    }
}

rootProject.name = "RCApp"
include(":app")