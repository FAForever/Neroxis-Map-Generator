package generator;

import map.SCMap;
import map.TerrainMaterials;
import util.serialized.LightingSettings;
import util.serialized.WaterSettings;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public strictfp class Preview {

    static void generate(BufferedImage image, SCMap map) {
        Graphics2D graphics = image.createGraphics();
        TerrainMaterials materials = map.getBiome().getTerrainMaterials();
        for (int i = 0; i < materials.texturePaths.length - 1; i++) {
            if (!materials.texturePaths[i].isEmpty()) {
                BufferedImage layer = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                Graphics2D layerGraphics = layer.createGraphics();
                layerGraphics.setColor(materials.previewColors[i]);
                layerGraphics.fillRect(0, 0, 256, 256);
                BufferedImage shadedLayer = getShadedImage(layer, map, i);
                TexturePaint layerPaint = new TexturePaint(shadedLayer, new Rectangle2D.Float(0, 0, 256, 256));
                graphics.setPaint(layerPaint);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            }
        }
        BufferedImage waterLayer = getWaterLayer(map);
        TexturePaint layerPaint = new TexturePaint(waterLayer, new Rectangle2D.Float(0, 0, 256, 256));
        graphics.setPaint(layerPaint);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    static BufferedImage getShadedImage(BufferedImage image, SCMap map, int layerIndex) {
        LightingSettings lightingSettings = map.getBiome().getLightingSettings();
        BufferedImage heightMap = map.getHeightmap();
        BufferedImage heightMapScaled = new BufferedImage(256, 256, BufferedImage.TYPE_USHORT_GRAY);
        AffineTransform atHeight = new AffineTransform();
        atHeight.scale(256f / heightMap.getWidth(), 256f / heightMap.getHeight());
        AffineTransformOp scaleOpHeight = new AffineTransformOp(atHeight, AffineTransformOp.TYPE_BILINEAR);
        heightMapScaled = scaleOpHeight.filter(heightMap, heightMapScaled);

        BufferedImage textureLowMap = map.getTextureMasksLow();
        BufferedImage textureLowScaled = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        AffineTransform atTextureLow = new AffineTransform();
        atTextureLow.scale(256f / textureLowMap.getWidth(), 256f / textureLowMap.getHeight());
        AffineTransformOp scaleOpTextureLow = new AffineTransformOp(atTextureLow, AffineTransformOp.TYPE_BILINEAR);
        textureLowScaled = scaleOpTextureLow.filter(textureLowMap, textureLowScaled);

        BufferedImage textureHighMap = map.getTextureMasksHigh();
        BufferedImage textureHighScaled = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        AffineTransform atTextureHigh = new AffineTransform();
        atTextureHigh.scale(256f / textureHighMap.getWidth(), 256f / textureHighMap.getHeight());
        AffineTransformOp scaleOpTextureHigh = new AffineTransformOp(atTextureHigh, AffineTransformOp.TYPE_BILINEAR);
        textureHighScaled = scaleOpTextureLow.filter(textureHighMap, textureHighScaled);

        float landDiffuseCoefficient = .9f;
        float landSpecularCoefficient = .75f;
        float landShininess = 1f;
        float azimuth = lightingSettings.SunDirection.getAzimuth() - 90f;
        float elevation = lightingSettings.SunDirection.getElevation();
        int imageHeight = heightMapScaled.getHeight();
        int imageWidth = heightMapScaled.getWidth();
        int xOffset = (int) StrictMath.round(StrictMath.sin(StrictMath.toRadians(azimuth)));
        int yOffset = (int) StrictMath.round(StrictMath.cos(StrictMath.toRadians(azimuth)));

        int[] textureAlphas = new int[4];
        int[] origRGBA = new int[4];
        int[] newRGBA = new int[4];
        int relativeLayerIndex;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
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

                    float slope = (heightArray1[0] - heightArray2[0]) * SCMap.HEIGHTMAP_SCALE;
                    float slopeAngle = (float) (180f - StrictMath.toDegrees(StrictMath.atan2(slope, StrictMath.sqrt(xOffset * xOffset + yOffset * yOffset))));
                    float normalAngle = slopeAngle - 90;
                    float reflectedAngle = normalAngle * 2 - elevation;
                    float diffuseTerm = (float) (StrictMath.max(StrictMath.cos(StrictMath.toRadians(normalAngle - elevation)) * landDiffuseCoefficient, 0));
                    float specularTerm = (float) (StrictMath.max(StrictMath.pow(StrictMath.cos(StrictMath.toRadians(90 - reflectedAngle)), landShininess) * landSpecularCoefficient, 0));

                    newRGBA[0] = (int) (origRGBA[0] * (lightingSettings.SunColor.x * (diffuseTerm + specularTerm)) + lightingSettings.SunAmbience.x);
                    newRGBA[1] = (int) (origRGBA[1] * (lightingSettings.SunColor.y * (diffuseTerm + specularTerm)) + lightingSettings.SunAmbience.y);
                    newRGBA[2] = (int) (origRGBA[2] * (lightingSettings.SunColor.z * (diffuseTerm + specularTerm)) + lightingSettings.SunAmbience.z);
                } else {
                    newRGBA = origRGBA.clone();
                }

                newRGBA[0] = StrictMath.max(StrictMath.min(newRGBA[0], 255), 0);
                newRGBA[1] = StrictMath.max(StrictMath.min(newRGBA[1], 255), 0);
                newRGBA[2] = StrictMath.max(StrictMath.min(newRGBA[2], 255), 0);
                if (relativeLayerIndex > 0) {
                    newRGBA[3] = StrictMath.max(StrictMath.min(textureAlphas[relativeLayerIndex - 1], 255), 0);
                }
                image.getRaster().setPixel(x, y, newRGBA);
            }
        }
        return image;
    }

    static BufferedImage getWaterLayer(SCMap map) {
        Color shallowColor = new Color(134, 233, 233);
        Color abyssColor = new Color(35, 49, 162);
        LightingSettings lightingSettings = map.getBiome().getLightingSettings();
        WaterSettings waterSettings = map.getBiome().getWaterSettings();
        BufferedImage heightMap = map.getHeightmap();
        BufferedImage heightMapScaled = new BufferedImage(256, 256, BufferedImage.TYPE_USHORT_GRAY);
        AffineTransform atHeight = new AffineTransform();
        atHeight.scale(256f / heightMap.getWidth(), 256f / heightMap.getHeight());
        AffineTransformOp scaleOpHeight = new AffineTransformOp(atHeight, AffineTransformOp.TYPE_BILINEAR);
        heightMapScaled = scaleOpHeight.filter(heightMap, heightMapScaled);
        BufferedImage waterLayer = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D waterLayerGraphics = waterLayer.createGraphics();

        float elevation = lightingSettings.SunDirection.getElevation();
        float[] mapElevations = {map.getBiome().getWaterSettings().Elevation, map.getBiome().getWaterSettings().ElevationAbyss};
        float waterDiffuseCoefficient = 1f;
        float waterSpecularCoefficient = 1f;
        float waterShininess = 1f;
        int[] heightArray = new int[1];
        int[] newRGBA = new int[4];
        BufferedImage layer = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < waterLayer.getHeight(); y++) {
            for (int x = 0; x < waterLayer.getWidth(); x++) {
                heightMapScaled.getRaster().getPixel(x, y, heightArray);

                float mapElevation = heightArray[0] * SCMap.HEIGHTMAP_SCALE;
                float weight = StrictMath.min(StrictMath.max((mapElevations[0] - mapElevation) / (mapElevations[0] - mapElevations[1]), 0), 1);
                float diffuseTerm = (float) (StrictMath.max(StrictMath.cos(StrictMath.toRadians(elevation)) * waterDiffuseCoefficient, 0));
                float specularTerm = (float) (StrictMath.max(StrictMath.pow(StrictMath.cos(StrictMath.toRadians(180 - elevation)), waterShininess) * waterSpecularCoefficient, 0));

                newRGBA[0] = (int) (shallowColor.getRed() * (1 - weight) + abyssColor.getRed() * weight);
                newRGBA[1] = (int) (shallowColor.getGreen() * (1 - weight) + abyssColor.getGreen() * weight);
                newRGBA[2] = (int) (shallowColor.getBlue() * (1 - weight) + abyssColor.getBlue() * weight);
                newRGBA[0] *= (lightingSettings.SunColor.x + waterSettings.SurfaceColor.x) * (diffuseTerm + specularTerm);
                newRGBA[1] *= (lightingSettings.SunColor.y + waterSettings.SurfaceColor.y) * (diffuseTerm + specularTerm);
                newRGBA[2] *= (lightingSettings.SunColor.z + waterSettings.SurfaceColor.z) * (diffuseTerm + specularTerm);
                newRGBA[0] = StrictMath.min(255, newRGBA[0]);
                newRGBA[1] = StrictMath.min(255, newRGBA[1]);
                newRGBA[2] = StrictMath.min(255, newRGBA[2]);
                newRGBA[3] = (int) StrictMath.min(255 * weight, 255);

                layer.getRaster().setPixel(x, y, newRGBA);
            }
        }

        TexturePaint layerPaint = new TexturePaint(layer, new Rectangle2D.Float(0, 0, 256, 256));
        waterLayerGraphics.setPaint(layerPaint);
        waterLayerGraphics.fillRect(0, 0, 256, 256);
        return waterLayer;
    }

}
