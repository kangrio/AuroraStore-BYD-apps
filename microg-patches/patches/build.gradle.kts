group = "com.kangrio"

patches {
    // TODO: Update this section with your project details.
    about {
        name = "MicroG Patches"
        description = "Patches for apps to support microg"
        source = "https://github.com/kangrio/AuroraStore-BYD-apps"
        author = "KangRio"
        contact = "na"
        website = "na"
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
val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    compileOnly(libs.gson)
    implementation(libs.morphe.patches.library)
    compileOnly("com.android.tools.build:apksig:9.1.1")
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}