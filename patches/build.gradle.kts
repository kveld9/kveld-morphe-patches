group = "com.kveld9.morphe"

patches {
    about {
        name = "Brave Patches"
        description = "Patches for Brave Browser: Brave Origin unlock, telemetry blocking, and performance optimizations."
        source = "https://github.com/kveld9/brave-patches"
        author = "kveld9"
        contact = "https://github.com/kveld9/brave-patches/issues"
        website = "https://github.com/kveld9/brave-patches"
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
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn("buildAndroid")

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
