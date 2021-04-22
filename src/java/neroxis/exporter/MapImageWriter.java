package neroxis.exporter;

import neroxis.importer.MapImporter;
import neroxis.importer.SaveImporter;
import neroxis.map.*;
import neroxis.util.ArgumentParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public strictfp class MapImageWriter {

    public static boolean DEBUG = false;

    private Path inMapPath;
    private SCMap map;
    private boolean writeOnlySelectImages;
    private String writeImagesPath;
    private boolean writeLayer0;
    private boolean writeLayer1;
    private boolean writeLayer2;
    private boolean writeLayer3;
    private boolean writeLayer4;
    private boolean writeLayer5;
    private boolean writeLayer6;
    private boolean writeLayer7;
    private boolean writeLayer8;
    private boolean writeLayerh;

    private SymmetrySettings symmetrySettings;

    public static void main(String[] args) throws IOException {

        Locale.setDefault(Locale.ROOT);

        MapImageWriter mapImageWriter = new MapImageWriter();

        mapImageWriter.interpretArguments(args);

        System.out.println("Creating map image files at " + mapImageWriter.inMapPath);
        mapImageWriter.importMap();
        mapImageWriter.writeMapImages();
    }

    public void interpretArguments(String[] args) {
        interpretArguments(ArgumentParser.parse(args));
    }

    private void interpretArguments(Map<String, String> arguments) {
        if (arguments.containsKey("help")) {
            System.out.println("map-image-writer usage:\n" +
                    "--help                 produce help message\n" +
                    "--in-folder-path arg   required, set the input folder for the map - the images will appear in this folder\n" +
                    "--create-only arg      optional, create only arg limits which layers have PNG images created from them (0, 1, 2, 3, 4, 5, 6, 7, 8, h)\n" +
                    " - ie: to create all available PNG images except the one for layer 7, use: --create-only 01234568h\n" +
                    " - options 0 - 8 will create PNG's of the corresponding texture layers, and option h will create a PNG of the heightmap" +
                    "--debug                optional, turn on debugging options\n");
            System.exit(0);
        }

        if (arguments.containsKey("debug")) {
            DEBUG = true;
        }

        if (!arguments.containsKey("in-folder-path")) {
            System.out.println("Input Folder not Specified");
            System.exit(1);
        }

        inMapPath = Paths.get(arguments.get("in-folder-path"));
        writeImagesPath = arguments.get("in-folder-path");
        symmetrySettings = new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE);
        writeOnlySelectImages = arguments.containsKey("create-only");
        if (writeOnlySelectImages) {
            String whichImages = arguments.get("create-only");
            if (whichImages != null) {
                writeLayer0 = whichImages.contains("0");
                writeLayer1 = whichImages.contains("1");
                writeLayer2 = whichImages.contains("2");
                writeLayer3 = whichImages.contains("3");
                writeLayer4 = whichImages.contains("4");
                writeLayer5 = whichImages.contains("5");
                writeLayer6 = whichImages.contains("6");
                writeLayer7 = whichImages.contains("7");
                writeLayer8 = whichImages.contains("8");
                writeLayerh = whichImages.contains("h");
            }
        } else {
            writeLayer0 = true;
            writeLayer1 = true;
            writeLayer2 = true;
            writeLayer3 = true;
            writeLayer4 = true;
            writeLayer5 = true;
            writeLayer6 = true;
            writeLayer7 = true;
            writeLayer8 = true;
            writeLayerh = true;
        }
    }

    public void importMap() {
        try {
            File dir = inMapPath.toFile();

            File[] mapFiles = dir.listFiles((dir1, filename) -> filename.endsWith(".scmap"));
            if (mapFiles == null || mapFiles.length == 0) {
                System.out.println("No scmap file in map folder");
                return;
            }
            map = MapImporter.importMap(inMapPath);
            SaveImporter.importSave(inMapPath, map);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while importing the map.");
        }
    }

    public void writeMapImages() throws IOException {

        Random random = new Random();
        FloatMask heightmapBase = map.getHeightMask(symmetrySettings);
        heightmapBase = new FloatMask(heightmapBase, random.nextLong(), "heightmapBase");
        map.setHeightImage(heightmapBase);

        FloatMask[] texturesMasks = map.getTextureMasksScaled(symmetrySettings);
        if(writeLayerh) {
            neroxis.util.ImageUtils.writePNGFromMask(heightmapBase, 1, Paths.get(writeImagesPath + "\\Heightmap.png"));
        }
        if(writeLayer1) {
            neroxis.util.ImageUtils.writePNGFromMask(texturesMasks[0], 255, Paths.get(writeImagesPath + "\\Layer 1.png"));
        }
        if(writeLayer2) {
            neroxis.util.ImageUtils.writePNGFromMask(texturesMasks[1], 255, Paths.get(writeImagesPath + "\\Layer 2.png"));
        }
        if(writeLayer3) {
            neroxis.util.ImageUtils.writePNGFromMask(texturesMasks[2], 255, Paths.get(writeImagesPath + "\\Layer 3.png"));
        }
        if(writeLayer4) {
            neroxis.util.ImageUtils.writePNGFromMask(texturesMasks[3], 255, Paths.get(writeImagesPath + "\\Layer 4.png"));
        }
        if(writeLayer5) {
            neroxis.util.ImageUtils.writePNGFromMask(texturesMasks[4], 255, Paths.get(writeImagesPath + "\\Layer 5.png"));
        }
        if(writeLayer6) {
            neroxis.util.ImageUtils.writePNGFromMask(texturesMasks[5], 255, Paths.get(writeImagesPath + "\\Layer 6.png"));
        }
        if(writeLayer7) {
            neroxis.util.ImageUtils.writePNGFromMask(texturesMasks[6], 255, Paths.get(writeImagesPath + "\\Layer 7.png"));
        }
        if(writeLayer8) {
            neroxis.util.ImageUtils.writePNGFromMask(texturesMasks[7], 255, Paths.get(writeImagesPath + "\\Layer 8.png"));
        }
        if(writeLayer0) {
            int mapImageSize = texturesMasks[0].getSize();
            texturesMasks[0].clampMin(0f).clampMax(1f).setSize(mapImageSize);
            texturesMasks[1].clampMin(0f).clampMax(1f).setSize(mapImageSize);
            texturesMasks[2].clampMin(0f).clampMax(1f).setSize(mapImageSize);
            texturesMasks[3].clampMin(0f).clampMax(1f).setSize(mapImageSize);
            texturesMasks[4].clampMin(0f).clampMax(1f).setSize(mapImageSize);
            texturesMasks[5].clampMin(0f).clampMax(1f).setSize(mapImageSize);
            texturesMasks[6].clampMin(0f).clampMax(1f).setSize(mapImageSize);
            texturesMasks[7].clampMin(0f).clampMax(1f).setSize(mapImageSize);
            FloatMask oldLayer0 = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings, "oldLayer0");
            oldLayer0.init(new BooleanMask(mapImageSize, random.nextLong(), symmetrySettings, "oldLayer0Inverted").invert(), 0f, 1f).subtract(texturesMasks[7]).subtract(texturesMasks[6]).subtract(texturesMasks[5]).subtract(texturesMasks[4]).subtract(texturesMasks[3]).subtract(texturesMasks[2]).subtract(texturesMasks[1]).subtract(texturesMasks[0]).clampMin(0f);
            neroxis.util.ImageUtils.writePNGFromMask(oldLayer0, 255, Paths.get(writeImagesPath + "\\Layer 0.png"));
        }
    }
}