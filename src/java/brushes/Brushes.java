package brushes;

import lombok.Data;
import map.FloatMask;
import map.SymmetrySettings;
import util.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

@Data
public strictfp class Brushes {
    public static final String[] MOUNTAIN_BRUSHES = {"mountain1.png", "mountain2.png", "mountain3.png", "mountain4.png", "mountain5.png",
            "mountain6.png", "mountain7.png", "mountain8.png", "mountain9.png", "volcano2.png", "hill1.png", "hill2.png", "noise1.png", "noise2.png",
            "hawaii1.png", "hawaii2.png", "island.png", "volcano2.png"};
    public static final String[] HILL_BRUSHES = {"hill1.png", "hill2.png", "noise1.png", "noise2.png"};
    private static final String CUSTOM_BRUSHES_DIR = "/images/brushes/";

    public static final List<String> goodBrushes = Arrays.asList("mountain1.png", "mountain2.png", "mountain4.png", "mountain5.png", "mountain6.png", "volcano2.png");

    public static FloatMask loadBrush(String brushName, SymmetrySettings symmetrySettings) {
        try {
            BufferedImage image = ImageUtils.readImage(CUSTOM_BRUSHES_DIR.concat(brushName));
            return new FloatMask(image, null, symmetrySettings);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load brush");
        }
    }
}
