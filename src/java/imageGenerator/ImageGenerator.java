package imageGenerator;

import brushes.Brushes;
import map.*;
import util.ArgumentParser;
import util.FileUtils;
import util.Vector2f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public strictfp class ImageGenerator {

    public static boolean DEBUG = false;
    private String folderPath;
    private int size;
    private int numberToGenerate;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.US);
        if (DEBUG) {
            Path debugDir = Paths.get(".", "debug");
            FileUtils.deleteRecursiveIfExists(debugDir);
            Files.createDirectory(debugDir);
        }

        ImageGenerator imageGenerator = new ImageGenerator();

        imageGenerator.interpretArguments(args);

        System.out.println("Creating image files at " + imageGenerator.folderPath);
        imageGenerator.generate();
    }

    public void interpretArguments(String[] args) {
        interpretArguments(ArgumentParser.parse(args));
    }

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("image-generator usage:\n" +
                    "--help                 produce help message\n" +
                    "--folder-path arg      required, set the folder where the images will appear\n" +
                    "--size arg             required, set the size (side length) of the images\n" +
                    "--num arg              required, set the of images that will be generated\n" +
                    "--textures             optional, generate textures\n" +
                    "--brushes              optional, generate brushes\n" +
                    "--debug                optional, turn on debugging options\n");
            System.exit(0);
        }

        if (arguments.containsKey("debug")) {
            DEBUG = true;
        }

        if (!arguments.containsKey("folder-path")) {
            System.out.println("Folder path not Specified");
            System.exit(1);
        }

        folderPath = arguments.get("folder-path");
        size = Integer.parseInt(arguments.get("size"));
        numberToGenerate = Integer.parseInt(arguments.get("num"));
    }

    public void generateCustomBrushes(int size, int numberToGenerate) throws IOException {
        Random random = new Random();
        for (int i = 0; i < numberToGenerate; i++) {
            int brushListLength = Brushes.goodBrushes.size();
            int reducedSize = size * 2 / 3;
            int variationDistance = StrictMath.max(size - reducedSize - 3, 0);
            int center = size / 2;
            int mountainsBrushSize = size / 10;

            String brush1 = Brushes.goodBrushes.get(random.nextInt(brushListLength));
            String brush2 = Brushes.goodBrushes.get(random.nextInt(brushListLength));
            String brush3 = Brushes.goodBrushes.get(random.nextInt(brushListLength));
            String brush4 = Brushes.goodBrushes.get(random.nextInt(brushListLength));
            String brush5 = Brushes.goodBrushes.get(random.nextInt(brushListLength));

            BinaryMask base = new BinaryMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));

            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance), center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush1, random.nextFloat(), 1f, reducedSize);
            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance), center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush2, random.nextFloat(), 1f, reducedSize);
            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance), center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush3, random.nextFloat(), 1f, reducedSize);
            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance), center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush4, random.nextFloat(), 1f, reducedSize);
            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance), center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush5, random.nextFloat(), 1f, reducedSize);

            BinaryMask mountains = new BinaryMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            for (int x = 0; x < 10; x++) {
                Vector2f loc = base.getRandomPosition();
                if(loc == null) {
                    loc = new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance), center + random.nextInt(variationDistance) - random.nextInt(variationDistance));
                }
                mountains.guidedWalkWithBrush(loc, base.getRandomPosition(), brush1, mountainsBrushSize, 7, 0.1f, 1f, mountainsBrushSize / 2);
            }
            mountains.intersect(base);
            BinaryMask mountainsBase = mountains.copy().inflate(15);
            BinaryMask mountainsBaseEdge = mountainsBase.copy().inflate(15).minus(mountainsBase);

            FloatMask newBrush = new FloatMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            newBrush.useBrushWithinAreaWithDensity(mountains, brush2, variationDistance, 0.05f, (float) 5 + random.nextInt(30));
            newBrush.useBrushWithinAreaWithDensity(mountainsBase, brush2, variationDistance, 0.005f, (float) 5 + random.nextInt(30));
            newBrush.useBrushWithinAreaWithDensity(mountainsBaseEdge, brush2, variationDistance, 0.05f, (float) 0.25 * (5 + random.nextInt(30)));
            newBrush.min(0);
            if(newBrush.areAnyEdgesGreaterThan(0f)) {
                i = i - 1;
            } else {
                util.ImageUtils.writeAutoScaledPNGFromMask(newBrush, folderPath + "\\Brush " + (i + 1) + ".png");
            }
        }
    }

    public void generateCustomTextures(int size, int numberToGenerate) {

    }

    public void generate() throws IOException {
        generateCustomBrushes(size, numberToGenerate);
        generateCustomTextures(size, numberToGenerate);
    }
}
