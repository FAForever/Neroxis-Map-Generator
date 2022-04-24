package com.faforever.neroxis.exporter;

import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.TerrainMaterials;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.util.ImageUtil;
import static com.faforever.neroxis.util.ImageUtil.readImage;
import static com.faforever.neroxis.util.ImageUtil.scaleImage;
import com.faforever.neroxis.util.serial.LightingSettings;
import com.faforever.neroxis.util.serial.WaterSettings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public strictfp class PreviewGenerator {

    public static final int PREVIEW_SIZE = 256;
    public static final String BLANK_PREVIEW = "/images/generatedMapIcon.png";
    private static final String MASS_IMAGE = "/images/map_markers/mass.png";
    private static final String HYDRO_IMAGE = "/images/map_markers/hydro.png";
    private static final String ARMY_IMAGE = "/images/map_markers/army.png";

    public static void generatePreview(SCMap map, SymmetrySettings symmetrySettings) throws IOException {
        FloatMask heightmap = new FloatMask(map.getHeightmap(), null, symmetrySettings);
        FloatMask sunReflectance = heightmap.copyAsNormalMask()
                                            .copyAsDotProduct(map.getBiome().getLightingSettings().getSunDirection());
        List<FloatMask> textureMasks = new ArrayList<>();
        textureMasks.addAll(
                List.of(new Vector4Mask(map.getTextureMasksLow(), null, symmetrySettings, 1).splitComponentMasks()));
        textureMasks.addAll(
                List.of(new Vector4Mask(map.getTextureMasksHigh(), null, symmetrySettings, 1).splitComponentMasks()));
        generatePreview(heightmap, sunReflectance, map, textureMasks.toArray(new FloatMask[0]));
    }

    public static void generatePreview(FloatMask heightmap, FloatMask sunReflectance, SCMap map,
                                       FloatMask... textureMasks) throws IOException {
        if (textureMasks.length != 8) {
            throw new IllegalArgumentException("Wrong number of textureMasks");
        }
        if (!map.isGeneratePreview()) {
            generateBlankPreview(map);
            return;
        }
        BufferedImage previewImage = map.getPreview();
        Graphics2D graphics = previewImage.createGraphics();
        TerrainMaterials materials = map.getBiome().getTerrainMaterials();
        List<FloatMask> scaledTextures = new ArrayList<>(Arrays.asList(textureMasks));
        FloatMask baseLayer = new FloatMask(PREVIEW_SIZE, null, new SymmetrySettings(Symmetry.NONE)).add(1f);
        scaledTextures.add(0, baseLayer);
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            if (!materials.getTexturePaths()[i].isEmpty()) {
                BufferedImage layer = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D layerGraphics = layer.createGraphics();
                layerGraphics.setColor(new Color(materials.getPreviewColors()[i]));
                layerGraphics.fillRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
                shadeLayer(layer, map, sunReflectance, scaledTextures.get(i));
                TexturePaint layerPaint = new TexturePaint(layer,
                                                           new Rectangle2D.Float(0, 0, PREVIEW_SIZE, PREVIEW_SIZE));
                graphics.setPaint(layerPaint);
                graphics.fillRect(0, 0, previewImage.getWidth(), previewImage.getHeight());
            }
        }
        BufferedImage waterLayer = getWaterLayer(map, sunReflectance, heightmap);
        TexturePaint layerPaint = new TexturePaint(waterLayer, new Rectangle2D.Float(0, 0, PREVIEW_SIZE, PREVIEW_SIZE));
        graphics.setPaint(layerPaint);
        graphics.fillRect(0, 0, previewImage.getWidth(), previewImage.getHeight());
    }

    public static void generateBlankPreview(SCMap map) throws IOException {
        BufferedImage blindPreview = readImage(BLANK_PREVIEW);
        map.getPreview().setData(blindPreview.getData());
    }

    private static BufferedImage shadeLayer(BufferedImage image, SCMap map, FloatMask reflectance,
                                            FloatMask textureLayer) {
        LightingSettings lightingSettings = map.getBiome().getLightingSettings();

        float ambientCoefficient = .25f;

        int[] origRGBA = new int[4];
        int[] newRGBA = new int[4];

        for (int y = 0; y < PREVIEW_SIZE; y++) {
            for (int x = 0; x < PREVIEW_SIZE; x++) {
                image.getRaster().getPixel(x, y, origRGBA);

                float coefficient = reflectance.getPrimitive(x, y) + ambientCoefficient;

                newRGBA[0] = (int) (origRGBA[0] * (lightingSettings.getSunColor().getX() * coefficient)
                                    + lightingSettings.getSunAmbience().getX());
                newRGBA[1] = (int) (origRGBA[1] * (lightingSettings.getSunColor().getY() * coefficient)
                                    + lightingSettings.getSunAmbience().getY());
                newRGBA[2] = (int) (origRGBA[2] * (lightingSettings.getSunColor().getZ() * coefficient)
                                    + lightingSettings.getSunAmbience().getZ());

                newRGBA[0] = StrictMath.max(StrictMath.min(newRGBA[0], 255), 0);
                newRGBA[1] = StrictMath.max(StrictMath.min(newRGBA[1], 255), 0);
                newRGBA[2] = StrictMath.max(StrictMath.min(newRGBA[2], 255), 0);
                newRGBA[3] = (int) StrictMath.max(StrictMath.min(textureLayer.getPrimitive(x, y) * 255, 255), 0);

                image.getRaster().setPixel(x, y, newRGBA);
            }
        }
        return image;
    }

    private static BufferedImage getWaterLayer(SCMap map, FloatMask reflectance, FloatMask heightmap) {
        Color shallowColor = new Color(134, 233, 233);
        Color abyssColor = new Color(35, 49, 162);
        LightingSettings lightingSettings = map.getBiome().getLightingSettings();
        WaterSettings waterSettings = map.getBiome().getWaterSettings();

        BufferedImage waterLayer = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D waterLayerGraphics = waterLayer.createGraphics();

        float waterheight = waterSettings.getElevation();
        float abyssheight = waterSettings.getElevationAbyss();

        float ambientCoefficient = .5f;
        int[] newRGBA = new int[4];
        BufferedImage layer = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);

        int height = waterLayer.getHeight();
        int width = waterLayer.getWidth();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                float weight = StrictMath.min(
                        StrictMath.max((waterheight - heightmap.getPrimitive(x, y)) / (waterheight - abyssheight), 0),
                        1);

                float coefficient = reflectance.getPrimitive(x, y) + ambientCoefficient;

                newRGBA[0] = (int) (shallowColor.getRed() * (1 - weight) + abyssColor.getRed() * weight);
                newRGBA[1] = (int) (shallowColor.getGreen() * (1 - weight) + abyssColor.getGreen() * weight);
                newRGBA[2] = (int) (shallowColor.getBlue() * (1 - weight) + abyssColor.getBlue() * weight);
                newRGBA[0] *= (lightingSettings.getSunColor().getX() + waterSettings.getSurfaceColor().getX())
                              * coefficient;
                newRGBA[1] *= (lightingSettings.getSunColor().getY() + waterSettings.getSurfaceColor().getY())
                              * coefficient;
                newRGBA[2] *= (lightingSettings.getSunColor().getZ() + waterSettings.getSurfaceColor().getZ())
                              * coefficient;
                newRGBA[0] = StrictMath.min(255, newRGBA[0]);
                newRGBA[1] = StrictMath.min(255, newRGBA[1]);
                newRGBA[2] = StrictMath.min(255, newRGBA[2]);
                newRGBA[3] = waterheight > heightmap.getPrimitive(x, y) ? (int) (191 * weight + 32) : 0;

                layer.getRaster().setPixel(x, y, newRGBA);
            }
        }
        TexturePaint layerPaint = new TexturePaint(layer, new Rectangle2D.Float(0, 0, PREVIEW_SIZE, PREVIEW_SIZE));
        waterLayerGraphics.setPaint(layerPaint);
        waterLayerGraphics.fillRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
        return waterLayer;
    }

    public static BufferedImage addMarkers(BufferedImage image, SCMap map) throws IOException {
        int resourceImageSize = 5;
        BufferedImage mexImage = scaleImage(readImage(MASS_IMAGE), resourceImageSize, resourceImageSize);
        BufferedImage hydroImage = scaleImage(readImage(HYDRO_IMAGE), resourceImageSize, resourceImageSize);
        BufferedImage spawnImage = scaleImage(readImage(ARMY_IMAGE), resourceImageSize, resourceImageSize);
        addMarkerImages(map.getMexes(), mexImage, image, map);
        addMarkerImages(map.getHydros(), hydroImage, image, map);
        addMarkerImages(map.getSpawns(), spawnImage, image, map);
        return image;
    }

    private static void addMarkerImages(Collection<? extends Marker> markers, BufferedImage markerImage,
                                        BufferedImage preview, SCMap map) {
        markers.forEach(marker -> {
            int x = (int) (marker.getPosition().getX() / map.getSize() * PREVIEW_SIZE - markerImage.getWidth(null) / 2);
            int y = (int) (marker.getPosition().getZ() / map.getSize() * PREVIEW_SIZE
                           - markerImage.getHeight(null) / 2);
            if (ImageUtil.inImageBounds(x, y, preview)) {
                preview.getGraphics().drawImage(markerImage, x, y, null);
            }
        });
    }
}
