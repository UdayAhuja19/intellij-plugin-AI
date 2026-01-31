plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.aiclient"
version = providers.gradleProperty("pluginVersion").get()

// Java 17+ required for IntelliJ 2024.3
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // OkHttp for HTTP requests (replaces Ktor)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    
    // Gson for JSON serialization (replaces kotlinx.serialization)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // IntelliJ Platform dependencies
    intellijPlatform {
        val platformType = providers.gradleProperty("platformType").get()
        val platformVersion = providers.gradleProperty("platformVersion").get()
        
        create(platformType, platformVersion)
        
        bundledPlugins(
            "com.intellij.java"
        )
        
        instrumentationTools()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        
        ideaVersion {
            sinceBuild = "241"
            untilBuild = provider { null }
        }
    }
    
    signing {
        // Configure signing if publishing to JetBrains Marketplace
    }
    
    publishing {
        // Configure publishing if releasing to JetBrains Marketplace
    }
}

tasks {
    wrapper {
        gradleVersion = "8.5"
    }
    
    buildSearchableOptions {
        enabled = false
    }
    
    // Ensure Java compilation from src/main/java
    compileJava {
        options.encoding = "UTF-8"
    }
}
