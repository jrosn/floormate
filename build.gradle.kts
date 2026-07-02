plugins {
    kotlin("js") version "2.1.0"
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "parket.js"
            }
        }
        binaries.executable()
    }
}
