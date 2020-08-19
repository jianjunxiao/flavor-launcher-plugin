package com.ztzh.flavorlauncherplugin

import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage

class PluginImpl implements Plugin<Project> {

    @Override
    void apply(Project project) {
        PluginExtension extension = project.extensions.create("flavorLauncherConfig", PluginExtension)
        if (project.android.hasProperty("applicationVariants")) {
            project.android.applicationVariants.all { variant ->
                String rootDir = variant.outputs.first().outputFile.parent.split("build").first()
                String flavor = variant.productFlavors[0].name
                String waterMask = variant.productFlavors[0].ext.label + variant.versionName
                if (flavor != extension.except) {
                    String taskName = "generateFlavorLauncher" + variant.name.capitalize()
                    println("======GenerateFlavorLauncher======: Task -> $taskName")
                    Task gfl = project.task(taskName).doLast {
                        generateLauncher(rootDir, flavor, waterMask, extension)
                    }
                    Task clean = project.tasks["clean"]
                    Task assemble = project.tasks["assemble${variant.name.capitalize()}"]
                    assemble.dependsOn(clean, gfl)
                }
            }
        }
    }

    private static void generateLauncher(String rootDir, String flavor, String waterMask, PluginExtension extension) {
        def targets = [
                "square": [
                        192: "src/${flavor}/res/mipmap-xxxhdpi/ic_launcher.png",
                        144: "src/${flavor}/res/mipmap-xxhdpi/ic_launcher.png",
                        96 : "src/${flavor}/res/mipmap-xhdpi/ic_launcher.png",
                        72 : "src/${flavor}/res/mipmap-hdpi/ic_launcher.png",
                        48 : "src/${flavor}/res/mipmap-mdpi/ic_launcher.png",
                ],
                "circle": [
                        192: "src/${flavor}/res/mipmap-xxxhdpi/ic_launcher_round.png",
                        144: "src/${flavor}/res/mipmap-xxhdpi/ic_launcher_round.png",
                        96 : "src/${flavor}/res/mipmap-xhdpi/ic_launcher_round.png",
                        72 : "src/${flavor}/res/mipmap-hdpi/ic_launcher_round.png",
                        48 : "src/${flavor}/res/mipmap-mdpi/ic_launcher_round.png"
                ]

        ]
        targets.each { target ->
            def original = ""
            if (target.key == "square") {
                original = extension.launcherPath
            }
            if (target.key == "circle") {
                original = extension.launcherRoundPath
            }
            target.value.each { item ->
                doGenerate(
                        rootDir + original,
                        rootDir + item.value,
                        item.key,
                        waterMask)
            }
        }
    }

    private static void doGenerate(String original, String output, int size, String waterMask) {

        File originalFile = new File(original)
        if (!originalFile.exists()) {
            println("======GenerateFlavorLauncher======: 源文件不存在")
            return
        }
        File outputFile = new File(output)
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }
        if (outputFile.exists()) {
            outputFile.delete()
        }

        BufferedImage bi = new BufferedImage(size, ((int) (size / 4)), BufferedImage.TYPE_INT_ARGB)
        Graphics2D g = bi.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.setColor(new Color(0f, 0f, 0f, 0.4f))
        g.fillRect(0, 0, bi.width, bi.height)

        g.setColor(Color.WHITE)
        Font font = new Font(null, Font.PLAIN, ((int) (size / 6.86f)))
        g.setFont(font)
        FontMetrics fm = g.getFontMetrics(font)
        String text = waterMask
        int left = (bi.width - fm.stringWidth(text)) / 2
        int bottom = bi.height / 2 - fm.height / 2 + font.size
        g.drawString(text, left, bottom)

        Thumbnails.of(originalFile)
                .imageType(BufferedImage.TYPE_INT_ARGB)
                .size(size, size)
                .watermark(Positions.CENTER, bi, 1f)
                .outputQuality(1f)
                .outputFormat("png")
                .toFile(outputFile)

        println("======GenerateFlavorLauncher======: Launcher生成成功，${outputFile.path}")
    }
}