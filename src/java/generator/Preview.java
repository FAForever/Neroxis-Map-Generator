package generator;

import map.BinaryMask;
import map.FloatMask;
import map.SCMap;

import java.awt.*;
import java.awt.image.BufferedImage;

public strictfp class Preview {

    static final float HEATMAP_DEADZONE = 0.1f;
    static final int HEATMAP_GRADIENT_STEPS = 16;

    static void generate(BufferedImage image, SCMap map, FloatMask lightGrassTexture, BinaryMask rock, BinaryMask grass, BinaryMask lightGrass){
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {

                int scaledX = x*2;
                int scaledY = y*2;

                float elevation =
                        ((float)map.getHeightmap().getRaster().getSample(scaledX,scaledY,0))
                                * SCMap.HEIGHTMAP_SCALE;

                float delta = elevation * (1/50f); // Magic number to make the colors more readable
                delta = (StrictMath.round(delta * HEATMAP_GRADIENT_STEPS + 0.5f) -0.5f) / HEATMAP_GRADIENT_STEPS;
                delta = (1f - HEATMAP_DEADZONE) - delta * (1f - HEATMAP_DEADZONE * 2f);

                float saturation = 0.9f;
                float value = 0.5f;

                saturation -= lightGrassTexture.get(x,y) * 0.4f; // Forests darken the preview a bit

                if (rock.get(x,y)){
                    delta *= 0.9f;
                }
                else if (
                        !grass.get(x,y) &&
                                !lightGrass.get(x,y) &&
                                !rock.get(x,y)
                ){
                    value -= 0.1f;	// Lakes appear darker
                }

                Color color = Color.getHSBColor(delta, saturation, value);
                image.setRGB(x, y, color.getRGB());
            }
        }
    }
}
