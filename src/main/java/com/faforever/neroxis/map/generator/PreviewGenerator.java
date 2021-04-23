package com.faforever.neroxis.map.generator;

import com.faforever.neroxis.map.Marker;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.TerrainMaterials;
import com.faforever.neroxis.util.ImageUtils;
import com.faforever.neroxis.util.serialized.LightingSettings;
import com.faforever.neroxis.util.serialized.WaterSettings;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

import static com.faforever.neroxis.util.ImageUtils.readImage;
import static com.faforever.neroxis.util.ImageUtils.scaleImage;

public strictfp class PreviewGenerator {

    private static final String MASS_IMAGE = "/images/map_markers/mass.png";
    private static final String HYDRO_IMAGE = "/images/map_markers/hydro.png";
    private static final String ARMY_IMAGE = "/images/map_markers/army.png";
    private static final int PREVIEW_SIZE = 256;

    public static void generatePreview(SCMap map) {
        BufferedImage previewImage = map.getPreview();
        Graphics2D graphics = previewImage.createGraphics();
        TerrainMaterials materials = map.getBiome().getTerrainMaterials();
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            if (!materials.getTexturePaths()[i].isEmpty()) {
                BufferedImage layer = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D layerGraphics = layer.createGraphics();
                layerGraphics.setColor(materials.getPreviewColors()[i]);
                layerGraphics.fillRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
                BufferedImage shadedLayer = getShadedImage(layer, map, i);
                TexturePaint layerPaint = new TexturePaint(shadedLayer, new Rectangle2D.Float(0, 0, PREVIEW_SIZE, PREVIEW_SIZE));
                graphics.setPaint(layerPaint);
                graphics.fillRect(0, 0, previewImage.getWidth(), previewImage.getHeight());
            }
        }
        BufferedImage waterLayer = getWaterLayer(map);
        TexturePaint layerPaint = new TexturePaint(waterLayer, new Rectangle2D.Float(0, 0, PREVIEW_SIZE, PREVIEW_SIZE));
        graphics.setPaint(layerPaint);
        graphics.fillRect(0, 0, previewImage.getWidth(), previewImage.getHeight());
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

    private static void addMarkerImages(Collection<? extends Marker> markers, BufferedImage markerImage, BufferedImage preview, SCMap map) {
        markers.forEach(marker -> {
            int x = (int) (marker.getPosition().getX() / map.getSize() * PREVIEW_SIZE - markerImage.getWidth(null) / 2);
            int y = (int) (marker.getPosition().getZ() / map.getSize() * PREVIEW_SIZE - markerImage.getHeight(null) / 2);
            if (ImageUtils.inImageBounds(x, y, preview)) {
                preview.getGraphics().drawImage(markerImage, x, y, null);
            }
        });
    }

    private static BufferedImage getShadedImage(BufferedImage image, SCMap map, int layerIndex) {
        LightingSettings lightingSettings = map.getBiome().getLightingSettings();
        BufferedImage heightMap = map.getHeightmap();
        BufferedImage heightMapScaled = scaleImage(heightMap, PREVIEW_SIZE, PREVIEW_SIZE);

        BufferedImage textureLowMap = map.getTextureMasksLow();
        BufferedImage textureLowScaled = scaleImage(textureLowMap, PREVIEW_SIZE, PREVIEW_SIZE);

        BufferedImage textureHighMap = map.getTextureMasksHigh();
        BufferedImage textureHighScaled = scaleImage(textureHighMap, PREVIEW_SIZE, PREVIEW_SIZE);

        float ambientCoefficient = .5f;
        float landDiffuseCoefficient = .5f;
        float landSpecularCoefficient = .5f;
        float landShininess = 1f;
        float azimuth = lightingSettings.getSunDirection().getAzimuth() + 90f;
        float elevation = lightingSettings.getSunDirection().getElevation();
        int imageHeight = heightMapScaled.getHeight();
        int imageWidth = heightMapScaled.getWidth();
        int xOffset = (int) StrictMath.round(StrictMath.sin(StrictMath.toRadians(azimuth)));
        int yOffset = (int) StrictMath.round(StrictMath.cos(StrictMath.toRadians(azimuth)));

        int[] textureAlphas = new int[4];
        int[] origRGBA = new int[4];
        int[] newRGBA = new int[4];
        int relativeLayerIndex;

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                if (layerIndex < 5) {
                    textureLowScaled.getRaster().getPixel(x, y, textureAlphas);
                    relativeLayerIndex = layerIndex;
                } else {
                    textureHighScaled.getRaster().getPixel(x, y, textureAlphas);
                    relativeLayerIndex = layerIndex - 4;
                }
                image.getRaster().getPixel(x, y, origRGBA);
                if (x - xOffset >= 0
                        && x - xOffset < imageWidth
                        && y - yOffset >= 0
                        && y - yOffset < imageHeight) {

                    int[] heightArray1 = new int[1];
                    int[] heightArray2 = new int[1];

                    heightMapScaled.getRaster().getPixel(x, y, heightArray1);
                    heightMapScaled.getRaster().getPixel(x - xOffset, y - yOffset, heightArray2);

                    float slope = (heightArray1[0] - heightArray2[0]) * map.getHeightMapScale();
                    float slopeAngle = (float) (180f - StrictMath.toDegrees(StrictMath.atan2(slope, StrictMath.sqrt(xOffset * xOffset + yOffset * yOffset))));
                    float normalAngle = slopeAngle - 90;
                    float reflectedAngle = normalAngle * 2 - elevation;
                    float diffuseTerm = (float) (StrictMath.max(StrictMath.cos(StrictMath.toRadians(normalAngle - elevation)) * landDiffuseCoefficient, 0));
                    float specularTerm = (float) (StrictMath.max(StrictMath.pow(StrictMath.cos(StrictMath.toRadians(90 - reflectedAngle)), landShininess) * landSpecularCoefficient, 0));

                    newRGBA[0] = (int) (origRGBA[0] * (lightingSettings.getSunColor().getX() * (ambientCoefficient + diffuseTerm + specularTerm)) + lightingSettings.getSunAmbience().getX());
                    newRGBA[1] = (int) (origRGBA[1] * (lightingSettings.getSunColor().getY() * (ambientCoefficient + diffuseTerm + specularTerm)) + lightingSettings.getSunAmbience().getY());
                    newRGBA[2] = (int) (origRGBA[2] * (lightingSettings.getSunColor().getZ() * (ambientCoefficient + diffuseTerm + specularTerm)) + lightingSettings.getSunAmbience().getZ());
                } else {
                    newRGBA = origRGBA.clone();
                }

                newRGBA[0] = StrictMath.max(StrictMath.min(newRGBA[0], 255), 0);
                newRGBA[1] = StrictMath.max(StrictMath.min(newRGBA[1], 255), 0);
                newRGBA[2] = StrictMath.max(StrictMath.min(newRGBA[2], 255), 0);
                if (relativeLayerIndex > 0) {
                    newRGBA[3] = StrictMath.max(StrictMath.min((int) ((textureAlphas[relativeLayerIndex - 1] - 128) / 127f * 255f), 255), 0);
                } else {
                    newRGBA[3] = 255;
                }
                image.getRaster().setPixel(x, y, newRGBA);
            }
        }
        return image;
    }

    private static BufferedImage getWaterLayer(SCMap map) {
        Color shallowColor = new Color(134, 233, 233);
        Color abyssColor = new Color(35, 49, 162);
        LightingSettings lightingSettings = map.getBiome().getLightingSettings();
        WaterSettings waterSettings = map.getBiome().getWaterSettings();
        BufferedImage heightMap = map.getHeightmap();
        BufferedImage heightMapScaled = scaleImage(heightMap, PREVIEW_SIZE, PREVIEW_SIZE);

        BufferedImage waterLayer = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D waterLayerGraphics = waterLayer.createGraphics();

        float elevation = lightingSettings.getSunDirection().getElevation();
        float[] mapElevations = {map.getBiome().getWaterSettings().getElevation(), map.getBiome().getWaterSettings().getElevationAbyss()};
        float ambientCoefficient = .5f;
        float waterDiffuseCoefficient = .25f;
        float waterSpecularCoefficient = .25f;
        float waterShininess = 1f;
        int[] heightArray = new int[1];
        int[] newRGBA = new int[4];
        BufferedImage layer = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < waterLayer.getHeight(); y++) {
            for (int x = 0; x < waterLayer.getWidth(); x++) {
                heightMapScaled.getRaster().getPixel(x, y, heightArray);

                float mapElevation = heightArray[0] * map.getHeightMapScale();
                float weight = StrictMath.min(StrictMath.max((mapElevations[0] - mapElevation) / (mapElevations[0] - mapElevations[1]), 0), 1);
                float diffuseTerm = (float) (StrictMath.max(StrictMath.cos(StrictMath.toRadians(elevation)) * waterDiffuseCoefficient, 0));
                float specularTerm = (float) (StrictMath.max(StrictMath.pow(StrictMath.cos(StrictMath.toRadians(180 - elevation)), waterShininess) * waterSpecularCoefficient, 0));

                newRGBA[0] = (int) (shallowColor.getRed() * (1 - weight) + abyssColor.getRed() * weight);
                newRGBA[1] = (int) (shallowColor.getGreen() * (1 - weight) + abyssColor.getGreen() * weight);
                newRGBA[2] = (int) (shallowColor.getBlue() * (1 - weight) + abyssColor.getBlue() * weight);
                newRGBA[0] *= (lightingSettings.getSunColor().getX() + waterSettings.getSurfaceColor().getX()) * (ambientCoefficient + diffuseTerm + specularTerm);
                newRGBA[1] *= (lightingSettings.getSunColor().getY() + waterSettings.getSurfaceColor().getY()) * (ambientCoefficient + diffuseTerm + specularTerm);
                newRGBA[2] *= (lightingSettings.getSunColor().getZ() + waterSettings.getSurfaceColor().getZ()) * (ambientCoefficient + diffuseTerm + specularTerm);
                newRGBA[0] = StrictMath.min(255, newRGBA[0]);
                newRGBA[1] = StrictMath.min(255, newRGBA[1]);
                newRGBA[2] = StrictMath.min(255, newRGBA[2]);
                newRGBA[3] = (int) StrictMath.min(255 * weight, 255);

                layer.getRaster().setPixel(x, y, newRGBA);
            }
        }

        TexturePaint layerPaint = new TexturePaint(layer, new Rectangle2D.Float(0, 0, PREVIEW_SIZE, PREVIEW_SIZE));
        waterLayerGraphics.setPaint(layerPaint);
        waterLayerGraphics.fillRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE);
        return waterLayer;
    }
}
