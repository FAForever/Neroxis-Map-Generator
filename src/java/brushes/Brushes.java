package brushes;

import lombok.Data;
import map.FloatMask;
import map.SymmetryHierarchy;
import util.ImageUtils;

import java.awt.image.BufferedImage;

@Data
public strictfp class Brushes {
    public static final String[] MOUNTAIN_BRUSHES = {"mountain1.png", "mountain2.png", "mountain3.png", "mountain4.png", "mountain5.png",
            "mountain6.png", "mountain7.png", "mountain8.png", "mountain9.png", "volcano2.png", "hill1.png", "hill2.png", "noise1.png", "noise2.png",
            "hawaii1.png", "hawaii2.png", "island.png", "volcano2.png", "Brush_1.png", "Brush_2.png", "Brush_3.png", "Brush_4.png", "Brush_5.png",
            "Brush_6.png", "Brush_7.png", "Brush_8.png", "Brush_9.png", "Brush_10.png", "Brush_11.png", "Brush_12.png", "Brush_13.png", "Brush_14.png",
            "Brush_15.png", "Brush_16.png", "Brush_17.png", "Brush_18.png", "Brush_19.png", "Brush_20.png", "Brush_21.png", "Brush_22.png", "Brush_23.png",
            "Brush_24.png", "Brush_25.png", "Brush_26.png", "Brush_27.png", "Brush_28.png", "Brush_29.png", "Brush_30.png", "Brush_31.png", "Brush_32.png",
            "Brush_33.png", "Brush_34.png", "Brush_35.png", "Brush_36.png", "Brush_37.png"};
    public static final String[] HILL_BRUSHES = {"hill1.png", "hill2.png", "noise1.png", "noise2.png", "Brush_1.png", "Brush_2.png", "Brush_3.png", "Brush_16.png",
            "Brush_21.png", "Brush_22.png", "Brush_23.png", "Brush_24.png", "Brush_25.png", "Brush_27.png", "Brush_29.png", "Brush_30.png", "Brush_31.png",
            "Brush_32.png", "Brush_33.png", "Brush_35.png", "Brush_36.png"};
    private static final String CUSTOM_BRUSHES_DIR = "/images/brushes/";

    public static FloatMask loadBrush(String brushName, SymmetryHierarchy symmetryHierarchy) {
        try {
            BufferedImage image = ImageUtils.readImage(CUSTOM_BRUSHES_DIR.concat(brushName));
            return new FloatMask(image, null, symmetryHierarchy);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load brush");
        }
    }
}
