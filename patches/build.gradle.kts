group = "com.kveld9.morphe"

patches {
    about {
        name = "kveld9 Patches"
        description = "Morphe patches for Brave Browser, Vivaldi Browser, and Gboard Lite: telemetry blocking, UI de-bloating, resource slimming, and performance optimizations."
        source = "https://github.com/kveld9/kveld-morphe-patches"
        author = "kveld9"
        contact = "https://github.com/kveld9/kveld-morphe-patches/issues"
        website = "https://github.com/kveld9/kveld-morphe-patches"
        license = "GPLv3"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    patchListGeneratorClasspath(libs.gson)
    patchListGeneratorClasspath("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn("buildAndroid")

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    register<JavaExec>("runPatchTest") {
        description = "Execute Morphe Patcher against target APK"

        dependsOn("buildAndroid")

        maxHeapSize = "8g"
        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchExecutionTestKt")

        if (project.hasProperty("app")) {
            systemProperty("targetApp", project.property("app").toString())
        }
        if (project.hasProperty("apk")) {
            systemProperty("targetApk", project.property("apk").toString())
        }
        if (project.hasProperty("out")) {
            systemProperty("outputApk", project.property("out").toString())
        }
        if (project.hasProperty("outputApk")) {
            systemProperty("outputApk", project.property("outputApk").toString())
        }
        System.getProperty("targetApp")?.let { systemProperty("targetApp", it) }
        System.getProperty("targetApk")?.let { systemProperty("targetApk", it) }
        System.getProperty("outputApk")?.let { systemProperty("outputApk", it) }
    }

    jar {
        exclude("util/**")
        finalizedBy("buildAndroid")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
