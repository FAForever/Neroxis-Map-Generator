package com.faforever.neroxis.exporter;

import com.faforever.neroxis.importer.MapImporter;
import com.faforever.neroxis.importer.SaveImporter;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.ArgumentParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.faforever.neroxis.util.ImageUtil.writePNGFromMask;

public strictfp class MapImageExporter {

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

        MapImageExporter mapImageExporter = new MapImageExporter();

        mapImageExporter.interpretArguments(args);

        System.out.println("Creating map image files at " + mapImageExporter.inMapPath);
        mapImageExporter.importMap();
        mapImageExporter.writeMapImages();
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
        FloatMask heightmapBase = new FloatMask(map.getHeightmap(), random.nextLong(), symmetrySettings, map.getHeightMapScale(), "heightmapBase");

        FloatMask[] textureMasksLow = new Vector4Mask(map.getTextureMasksLow(), random.nextLong(), symmetrySettings, 1f, "TextureMasksLow")
                .subtractScalar(128f).divideScalar(127f).clampComponentMin(0f).clampComponentMax(1f).splitComponentMasks();
        FloatMask[] textureMasksHigh = new Vector4Mask(map.getTextureMasksHigh(), random.nextLong(), symmetrySettings, 1f, "TextureMasksHigh")
                .subtractScalar(128f).divideScalar(127f).clampComponentMin(0f).clampComponentMax(1f).splitComponentMasks();
        if (writeLayerh) {
            writePNGFromMask(heightmapBase, 1, Paths.get(writeImagesPath).resolve("Heightmap.png"));
        }
        if (writeLayer1) {
            writePNGFromMask(textureMasksLow[0], 255, Paths.get(writeImagesPath).resolve("Layer 1.png"));
        }
        if (writeLayer2) {
            writePNGFromMask(textureMasksLow[1], 255, Paths.get(writeImagesPath).resolve("Layer 2.png"));
        }
        if (writeLayer3) {
            writePNGFromMask(textureMasksLow[2], 255, Paths.get(writeImagesPath).resolve("Layer 3.png"));
        }
        if (writeLayer4) {
            writePNGFromMask(textureMasksLow[3], 255, Paths.get(writeImagesPath).resolve("Layer 4.png"));
        }
        if (writeLayer5) {
            writePNGFromMask(textureMasksHigh[0], 255, Paths.get(writeImagesPath).resolve("Layer 5.png"));
        }
        if (writeLayer6) {
            writePNGFromMask(textureMasksHigh[1], 255, Paths.get(writeImagesPath).resolve("Layer 6.png"));
        }
        if(writeLayer7) {
            writePNGFromMask(textureMasksHigh[2], 255, Paths.get(writeImagesPath).resolve("Layer 7.png"));
        }
        if(writeLayer8) {
            writePNGFromMask(textureMasksHigh[3], 255, Paths.get(writeImagesPath).resolve("Layer 8.png"));
        }
        if(writeLayer0) {
            int mapImageSize = textureMasksLow[0].getSize();
            FloatMask oldLayer0 = new FloatMask(mapImageSize, random.nextLong(), symmetrySettings, "oldLayer0");
            oldLayer0.init(new BooleanMask(mapImageSize, random.nextLong(), symmetrySettings, "oldLayer0Inverted").invert(), 0f, 1f);
            Arrays.stream(textureMasksHigh).forEach(oldLayer0::subtract);
            Arrays.stream(textureMasksLow).forEach(oldLayer0::subtract);
            oldLayer0.clampMin(0f);
            writePNGFromMask(oldLayer0, 255, Paths.get(writeImagesPath).resolve("Layer 0.png"));
        }
    }
}