package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.OutputFolderMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.util.ImageUtil;
import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(name = "export-textures", mixinStandardHelpOptions = true, description = "Generate the helper textures", versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class MapEnvTextureExporter implements Callable<Integer> {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;
    @CommandLine.Mixin
    private OutputFolderMixin outputFolderMixin;

    @Override
    public Integer call() throws Exception {
        generate();
        return 0;
    }
    
    public void generate() throws Exception {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputFolderMixin.getOutputPath())) {
            BufferedImage pbrTexture = new BufferedImage(4096, 4096, BufferedImage.TYPE_INT_ARGB);
            for (Path path : stream) {
                System.out.print("processing " + path + "\n");
                if (Files.isRegularFile(path)) {
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(path.toFile().getName());
                    if (matcher.find()) {
                        BufferedImage image = ImageIO.read(path.toFile());
                        String numberStr = matcher.group();
                        int layer = Integer.parseInt(numberStr);
                        if (path.getFileName().toString().startsWith("Roughness")) {
                            System.out.print("Writing roughness texture\n");
                            pbrTexture = ImageUtil.setRoughnessMap(pbrTexture, image, layer);
                        } else if (path.getFileName().toString().startsWith("Height")) {
                            System.out.print("Writing height texture\n");
                            pbrTexture = ImageUtil.setHeightMap(pbrTexture, image, layer);
                        }
                    }
                }
            }
            Path textureDirectory = outputFolderMixin.getOutputPath();
            Path filePath = textureDirectory.resolve("heightRoughness.dds");
            Files.createDirectories(filePath.getParent());
            ImageUtil.writeCompressedDDS(pbrTexture, filePath);
        }
    }
}
