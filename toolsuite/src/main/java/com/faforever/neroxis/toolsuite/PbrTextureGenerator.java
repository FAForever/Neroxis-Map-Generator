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
    private Integer textureImageSize;
    @Getter
    private Path inputPath;
    @Getter
    private Path outputPath;

    @CommandLine.Option(names = "--size", defaultValue = "1024", description = "Size of the input textures in pixels. Defaults to 1024.")
    public void setTextureImageSize(int size) {
        if (!isPowerOfTwo(size)) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Texture size must be a power of two!");
        }
        textureImageSize = size;
    }

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

    @Override
    public Integer call() throws Exception {
        generatePbrTexture();
        return 0;
    }
    
    SymmetrySettings noSymmetry = new SymmetrySettings(Symmetry.NONE);

    public boolean isPowerOfTwo(int number) {
        if(number <=0){
            return false;
        }
        return (number & -number) == number;
    }
    
    public void generatePbrTexture() throws Exception {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(getInputPath())) {
            int pbrTextureSize = textureImageSize * 4;
            int offset = pbrTextureSize / 2;
            Vector4Mask pbrMask = new Vector4Mask(pbrTextureSize, 0L, noSymmetry);
            BufferedImage pbrTexture = new BufferedImage(pbrTextureSize, pbrTextureSize, BufferedImage.TYPE_INT_ARGB);
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
                            if (image.getHeight() != textureImageSize) {
                                throw new RuntimeException("Wrong texture size! Expected " + textureImageSize + ", but is " + image.getHeight() + ". " +
                                                           "All textures must be the same size. " +
                                                           "Don't forget to use the --size option if your textures are not 1024 px.");
                            }
                            FloatMask roughness = createOffsetMaskFromImage(image);
                            int component = (layer >= 4) ? 3 : 1;
                            int xOffset = (layer % 2 == 1) ? offset : 0;
                            int yOffset = (layer % 4 >= 2) ? offset : 0;
                            pbrMask.setComponentWithOffset(roughness, component, xOffset, yOffset, false, false);
                            filesProcessed++;
                        } else if (path.getFileName().toString().toLowerCase().startsWith("height")) {
                            System.out.printf("Reading height texture %s\n", path.getFileName());
                            if (image.getHeight() != textureImageSize) {
                                throw new RuntimeException("Wrong texture size! Expected " + textureImageSize + ", but is " + image.getHeight() + ". " +
                                                           "All textures must be the same size. " +
                                                           "Don't forget to use the --size option if your textures are not 1024 px.");
                            }
                            FloatMask height = createOffsetMaskFromImage(image);
                            int component = (layer >= 4) ? 2 : 0;
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
            pbrMask.writeToImage(pbrTexture);
            Path textureDirectory = getOutputPath();
            Path filePath = textureDirectory.resolve("heightRoughness.dds");
            System.out.printf("Processed %d files.\n", filesProcessed);
            System.out.print("Compressing dds texture. This will probably take a while...\n");
            ImageUtil.writeCompressedDDS(pbrTexture, filePath);
            System.out.print("Successfully wrote dds output\n");
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
