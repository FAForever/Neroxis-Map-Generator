package neroxis.generator;

import neroxis.brushes.Brushes;
import neroxis.map.BinaryMask;
import neroxis.map.FloatMask;
import neroxis.map.Symmetry;
import neroxis.map.SymmetrySettings;
import neroxis.util.ArgumentParser;
import neroxis.util.FileUtils;
import neroxis.util.Vector2f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public strictfp class ImageGenerator {

    public static boolean DEBUG = false;
    private String folderPath;
    private int size = 512;
    private int numberToGenerate = 1;
    private boolean textures;
    private boolean brushes;
    private float colorVariation = 25;
    private float redStrength = -1;
    private float greenStrength = -1;
    private float blueStrength = -1;
    private int levelOfDetail = 100;
    private int maxFeatureSize = 100;
    private boolean recursiveGenerationFlag = false;

    private FloatMask redMask;
    private FloatMask greenMask;
    private FloatMask blueMask;

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
                    "--brushes arg          optional, generate brushes - optional arg - if arg is 'recursive', brush generation will be recursive\n" +
                    "--textures             optional, generate textures\n" +
                    "--size arg             optional, set the size (side length) of images that will be generated\n" +
                    "--num arg              optional, set the number of images to generate\n" +
                    "--color-variation arg  optional, set the percent of color variation for textures that will be generated\n" +
                    "--red arg              optional, set the average percent strength of red for textures that will be generated\n" +
                    "--green arg            optional, set the average percent strength of green for textures that will be generated\n" +
                    "--blue arg             optional, set the average percent strength of blue for textures that will be generated\n" +
                    "--level-of-detail arg  optional, set the amount of fullness/detail for the textures that will be generated\n" +
                    "- positive numerical input - default is 100, but there is no limit (higher numbers will have higher processing times)\n" +
                    "--max-feature-size arg optional, set the maximum size of features/details for the textures that will be generated\n" +
                    "- positive numerical input - default is 100, but there is no limit\n" +
                    "--debug                optional, turn on debugging options\n" +
                    "*** Note that generating images will overwrite previously made images of the same name in the same folder ***");
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

        if (arguments.containsKey("size")) {
            size = Integer.parseInt(arguments.get("size"));
        }

        if (arguments.containsKey("num")) {
            numberToGenerate = Integer.parseInt(arguments.get("num"));
        }

        if (arguments.containsKey("brushes")) {
            brushes = true;
            String brushesArg = arguments.get("brushes");
            if (brushesArg != null) {
                if (brushesArg.equals("recursive")) {
                    recursiveGenerationFlag = true;
                }
            }
        }

        if (arguments.containsKey("textures")) {
            textures = true;
        }

        if (arguments.containsKey("color-variation")) {
            colorVariation = Float.parseFloat(arguments.get("color-variation"));
        }

        if (arguments.containsKey("red")) {
            redStrength = Float.parseFloat(arguments.get("red"));
        }

        if (arguments.containsKey("green")) {
            greenStrength = Float.parseFloat(arguments.get("green"));
        }

        if (arguments.containsKey("blue")) {
            blueStrength = Float.parseFloat(arguments.get("blue"));
        }

        if (arguments.containsKey("level-of-detail")) {
            levelOfDetail = Integer.parseInt(arguments.get("level-of-detail"));
        }

        if (arguments.containsKey("max-feature-size")) {
            maxFeatureSize = Integer.parseInt(arguments.get("max-feature-size"));
        }
    }

    public void generateCustomBrushes(int size, int numberToGenerate, boolean recursiveGeneration) throws IOException {

        String brush1 = null;
        String brush2 = null;
        String brush3 = null;
        String brush4 = null;
        String brush5 = null;

        FloatMask recursiveBrush1 = null;
        FloatMask recursiveBrush2 = null;
        FloatMask recursiveBrush3 = null;
        FloatMask recursiveBrush4 = null;
        FloatMask recursiveBrush5 = null;
        FloatMask secondNewestBrush = null;
        FloatMask newestBrush = null;

        for (int i = 0; i < numberToGenerate; i++) {
            int brushListLength = Brushes.GENERATOR_BRUSHES.size();
            int reducedSize = size * 2 / 3;
            int variationDistance = StrictMath.max(size - reducedSize - 3, 0);
            int center = size / 2;
            int mountainsBrushSize = size / 10;
            Random random = new Random();

            if (!recursiveGeneration || i < 5) {
                brush1 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(brushListLength));
                brush2 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(brushListLength));
                brush3 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(brushListLength));
                brush4 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(brushListLength));
                brush5 = Brushes.GENERATOR_BRUSHES.get(random.nextInt(brushListLength));
            }

            BinaryMask base = new BinaryMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));

            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance),
                    center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush1, recursiveBrush1, random.nextFloat(), 1f, reducedSize);
            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance),
                    center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush2, recursiveBrush2, random.nextFloat(), 1f, reducedSize);
            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance),
                    center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush3, recursiveBrush3, random.nextFloat(), 1f, reducedSize);
            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance),
                    center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush4, recursiveBrush4, random.nextFloat(), 1f, reducedSize);
            base.combineBrush(new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance),
                    center + random.nextInt(variationDistance) - random.nextInt(variationDistance)), brush5, recursiveBrush5, random.nextFloat(), 1f, reducedSize);

            BinaryMask mountains = new BinaryMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            for (int x = 0; x < 10; x++) {
                Vector2f loc = base.getRandomPosition();
                if(loc == null) {
                    loc = new Vector2f(center + random.nextInt(variationDistance) - random.nextInt(variationDistance), center + random.nextInt(variationDistance) - random.nextInt(variationDistance));
                }
                mountains.guidedWalkWithBrush(loc, base.getRandomPosition(), brush1, secondNewestBrush, mountainsBrushSize, 7, 0.1f, 1f, mountainsBrushSize / 2);
            }
            mountains.intersect(base);
            BinaryMask mountainsBase = mountains.copy().inflate(15);
            BinaryMask mountainsBaseEdge = mountainsBase.copy().inflate(15).minus(mountainsBase);

            FloatMask newBrush = new FloatMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            newBrush.useBrushWithinAreaWithDensity(mountains, brush2, newestBrush, variationDistance, 0.05f, (float) 5 + random.nextInt(30));
            newBrush.useBrushWithinAreaWithDensity(mountainsBase, brush2, newestBrush, variationDistance, 0.005f, (float) 5 + random.nextInt(30));
            newBrush.useBrushWithinAreaWithDensity(mountainsBaseEdge, brush2, newestBrush, variationDistance, 0.05f, (float) 0.25 * (5 + random.nextInt(30)));
            newBrush.min(0);
            if(newBrush.areAnyEdgesGreaterThan(0f)) {
                i = i - 1;
            } else {
                if (recursiveGeneration){
                    FloatMask newBrushSmoothed = newBrush.copy().smooth(8).min(0);
                    if(newBrushSmoothed.areAnyEdgesGreaterThan(0f)) {
                        i = i - 1;
                    } else {
                        newBrush = newBrush.resizeContentToFillVoidBelowThreshold(0f, true);
                        neroxis.util.ImageUtils.writeAutoScaledPNGFromMask(newBrush, Paths.get(folderPath + "\\Brush_" + (i + 1) + ".png"));
                        newBrushSmoothed.resizeContentToFillVoidBelowThreshold(0f, true);
                        switch (i % 5) {
                            case 0:
                                recursiveBrush1 = newBrushSmoothed;
                            case 1:
                                recursiveBrush2 = newBrushSmoothed;
                            case 2:
                                recursiveBrush3 = newBrushSmoothed;
                            case 3:
                                recursiveBrush4 = newBrushSmoothed;
                            case 4:
                                recursiveBrush5 = newBrushSmoothed;
                        }
                        secondNewestBrush = newestBrush;
                        newestBrush = newBrushSmoothed;
                    }
                } else {
                    newBrush = newBrush.resizeContentToFillVoidBelowThreshold(0f, true);
                    neroxis.util.ImageUtils.writeAutoScaledPNGFromMask(newBrush, Paths.get(folderPath + "\\Brush_" + (i + 1) + ".png"));
                }
            }
        }
    }

    public void generateCustomTextures(int size, int numberToGenerate, float colorVariation) throws IOException {

        float redLocus;
        float greenLocus;
        float blueLocus;

        for (int i = 0; i < numberToGenerate; i++) {

            int brushListLength = Brushes.GENERATOR_BRUSHES.size();
            Random random = new Random();
            boolean tooEmpty = true;

            redMask = new FloatMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            greenMask = new FloatMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            blueMask = new FloatMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));

            BinaryMask wholeImage = new BinaryMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            wholeImage.fillRect(0,0, size, size, true);
            BinaryMask areaToTexture = wholeImage;

            if(redStrength == -1) {
                redLocus = random.nextFloat();
            } else {
                redLocus = redStrength / 100;
            }
            if(greenStrength == -1) {
                greenLocus = random.nextFloat();
            } else {
                greenLocus = greenStrength / 100;
            }
            if(blueStrength == -1) {
                blueLocus = random.nextFloat();
            } else {
                blueLocus = blueStrength / 100;
            }

            for (int a = 0; a < levelOfDetail; a++) {
                int chainBrushSize = random.nextInt(maxFeatureSize) + 1;
                int chainTextureBrushSize = random.nextInt(maxFeatureSize) + 1;
                BinaryMask chain = new BinaryMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
                FloatMask chainTexture = new FloatMask(size, random.nextLong(), new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
                if(a > 0.75 * levelOfDetail && tooEmpty) {
                    FloatMask wholeImageTexture = redMask.copy().add(greenMask).add(blueMask);
                    areaToTexture = wholeImageTexture.convertToBinaryMask(0f, 0.1f);
                    if(areaToTexture.getCount() < size * 3) {
                        tooEmpty = false;
                        areaToTexture = wholeImage;
                    }
                }
                LinkedList<Vector2f> possibleLocations = areaToTexture.getAllCoordinatesEqualTo(true, 1);
                int numPossibleLocations = possibleLocations.size();
                for (int x = 0; x < 5; x++) {
                    Vector2f loc = possibleLocations.get(random.nextInt(numPossibleLocations));
                    while(loc == null) {
                        loc = wholeImage.getRandomPosition();
                    }
                    Vector2f target = possibleLocations.get(random.nextInt(numPossibleLocations));
                    while(target == null) {
                        target = wholeImage.getRandomPosition();
                    }
                    chain.guidedWalkWithBrushWrapEdges(loc, target, Brushes.GENERATOR_BRUSHES.get(random.nextInt(brushListLength)), chainBrushSize,
                            random.nextInt(15) + 1, 0.1f, 1f, chainBrushSize / 2);
                }
                chainTexture.useBrushWithinAreaWithDensityWrapEdges(chain, Brushes.GENERATOR_BRUSHES.get(random.nextInt(brushListLength)), chainTextureBrushSize, 0.05f, 5 * random.nextFloat());

                float redWeight = redLocus + ((random.nextBoolean() ? 1 : - 1) * random.nextFloat() * colorVariation / 100);
                float greenWeight = greenLocus + ((random.nextBoolean() ? 1 : - 1) * random.nextFloat() * colorVariation / 100);
                float blueWeight = blueLocus + ((random.nextBoolean() ? 1 : - 1) * random.nextFloat() * colorVariation / 100);

                if(redWeight < 0) { redWeight = 0;}
                if(greenWeight < 0) { greenWeight = 0;}
                if(blueWeight < 0) { blueWeight = 0;}

                color(redWeight, greenWeight, blueWeight, chainTexture);
            }
            neroxis.util.ImageUtils.writeAutoScaledPNGFromMasks(redMask, greenMask, blueMask, Paths.get(folderPath + "\\Texture_" + (i + 1) + ".png"));
        }
    }

    private void color(float redPercent, float greenPercent, float bluePercent, FloatMask other) {
        redMask.addWeighted(other, redPercent);
        greenMask.addWeighted(other, greenPercent);
        blueMask.addWeighted(other, bluePercent);
    }

    private void colorScaled(float redPercent, float greenPercent, float bluePercent, FloatMask other, float scaleMultiplier) {
        redMask.addWeighted(other, redPercent);
        greenMask.addWeighted(other, greenPercent);
        blueMask.addWeighted(other, bluePercent);
    }

    public void generate() throws IOException {
        if(brushes) {
            generateCustomBrushes(size, numberToGenerate, recursiveGenerationFlag);
        }
        if(textures) {
            generateCustomTextures(size, numberToGenerate, colorVariation);
        }
    }
}
