group = "com.kveld9.morphe"

patches {
    about {
        name = "kveld9 Patches"
        description = "Morphe patches for Brave Browser and Gboard Lite: telemetry blocking, UI de-bloating, resource slimming, and performance optimizations."
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
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn("buildAndroid")

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    jar {
        exclude("util/**")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
