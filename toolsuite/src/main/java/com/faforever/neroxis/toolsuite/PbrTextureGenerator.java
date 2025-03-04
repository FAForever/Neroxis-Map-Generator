package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.CLIUtils;
import com.faforever.neroxis.cli.DebugMixin;
import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.ImageUtil;
import lombok.Getter;
import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(name = "generate-pbr", mixinStandardHelpOptions = true, 
        description = "Generate the pbr texture from individual height and roughness textures", 
        versionProvider = VersionProvider.class, usageHelpAutoWidth = true)
public class PbrTextureGenerator implements Callable<Integer> {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;
    @CommandLine.Mixin
    private DebugMixin debugMixin;
    @Getter
    private Path inputPath;
    @Getter
    private Path outputPath;

    @CommandLine.Option(names = {"--in-path"}, description = "Folder with input images. Defaults to the working directory.", defaultValue = ".")
    public void setInputPath(Path inputPath) {
        CLIUtils.checkWritableDirectory(inputPath, spec);
        this.inputPath = inputPath;
    }

    @CommandLine.Option(names = {"--out-path"}, description = "Folder to save the dds image to. Defaults to the working directory.", defaultValue = ".")
    public void setOutputPath(Path outputPath) {
        CLIUtils.checkWritableDirectory(outputPath, spec);
        this.outputPath = outputPath;
    }

    private int inputImageSize = 0;
    private Vector4Mask pbrMask;
    private int offset;

    @Override
    public Integer call() throws Exception {
        generatePbrTexture();
        return 0;
    }
    
    SymmetrySettings noSymmetry = new SymmetrySettings(Symmetry.NONE);
    
    public void generatePbrTexture() throws Exception {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(getInputPath())) {
            int filesProcessed = 0;
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(path.toFile().getName());
                    if (matcher.find()) {
                        BufferedImage image = ImageIO.read(path.toFile());
                        String numberStr = matcher.group();
                        int layer = Integer.parseInt(numberStr);
                        if (path.getFileName().toString().toLowerCase().startsWith("roughness")) {
                            System.out.printf("Reading roughness texture %s\n", path.getFileName());
                            validateSize(image.getHeight());
                            FloatMask roughness = createOffsetMaskFromImage(image);
                            int component = (layer >= 4) ? 2 : 0;
                            int xOffset = (layer % 2 == 1) ? offset : 0;
                            int yOffset = (layer % 4 >= 2) ? offset : 0;
                            pbrMask.setComponentWithOffset(roughness, component, xOffset, yOffset, false, false);
                            filesProcessed++;
                        } else if (path.getFileName().toString().toLowerCase().startsWith("height")) {
                            System.out.printf("Reading height texture %s\n", path.getFileName());
                            validateSize(image.getHeight());
                            FloatMask height = createOffsetMaskFromImage(image);
                            int component = (layer >= 4) ? 3 : 1;
                            int xOffset = (layer % 2 == 1) ? offset : 0;
                            int yOffset = (layer % 4 >= 2) ? offset : 0;
                            pbrMask.setComponentWithOffset(height, component, xOffset, yOffset, false, false);
                            filesProcessed++;
                        }
                    }
                }
            }
            if (filesProcessed == 0) {
                throw new RuntimeException("No files found to write into the pbr texture. " +
                        "The files need to be named 'RoughnessX' or 'HeightX' where X is the number " +
                        "that specifies the texture layer.");
            }
            BufferedImage pbrTexture = new BufferedImage(inputImageSize * 4, inputImageSize * 4, BufferedImage.TYPE_INT_ARGB);
            pbrMask.writeToImage(pbrTexture);
            Path textureDirectory = getOutputPath();
            Path filePath = textureDirectory.resolve("roughnessAndHeight.dds");
            System.out.printf("Processed %d files.\n", filesProcessed);
            System.out.print("Compressing dds texture. This can take over a minute...\n");
            ImageUtil.writeCompressedDDS(pbrTexture, filePath);
            System.out.print("Successfully wrote dds output\n");
        }
    }

    private void validateSize(int imageSize) {
        if (inputImageSize == 0) {
            inputImageSize = imageSize;
            offset = imageSize * 2;
            pbrMask = new Vector4Mask(imageSize * 4, 0L, noSymmetry);
        } else if (imageSize != inputImageSize) {
            throw new RuntimeException("Wrong texture size! Expected " + inputImageSize
                                       + ", but is " + imageSize + ". " +
                                       "All textures must be the same size.");
        }
    }

    private FloatMask createOffsetMaskFromImage(BufferedImage image) {
        // We need to get rid of multichannel images first
        BufferedImage image_gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        image_gray.getGraphics().drawImage(image, 0, 0, null);

        // We need to write the texture with padding. We can achieve that by offsetting it and writing it in a 2x2 grid
        FloatMask mask = new FloatMask(image_gray, 0L, noSymmetry);
        FloatMask roughness = new FloatMask(mask.getSize() * 2, 0L, noSymmetry);
        roughness.setWithOffset(mask, (int) (mask.getSize() * 0.5), (int) (mask.getSize() * 0.5), false, true);
        roughness.setWithOffset(mask, (int) (mask.getSize() * 1.5), (int) (mask.getSize() * 0.5), false, true);
        roughness.setWithOffset(mask, (int) (mask.getSize() * 0.5), (int) (mask.getSize() * 1.5), false, true);
        roughness.setWithOffset(mask, (int) (mask.getSize() * 1.5), (int) (mask.getSize() * 1.5), false, true);
        return roughness;
    }
}
