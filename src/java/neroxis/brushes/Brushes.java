package neroxis.brushes;

import lombok.Data;
import neroxis.map.FloatMask;
import neroxis.map.Symmetry;
import neroxis.map.SymmetrySettings;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Data
public strictfp class Brushes {
    public static final List<String> MOUNTAIN_BRUSHES = Arrays.asList("mountain1.png", "mountain2.png", "mountain3.png", "mountain4.png", "mountain5.png",
            "mountain6.png", "mountain7.png", "mountain8.png", "mountain9.png", "volcano2.png", "hill1.png", "hill2.png", "noise1.png", "noise2.png",
            "hawaii1.png", "hawaii2.png", "island.png", "volcano2.png");
    public static final List<String> HILL_BRUSHES = Arrays.asList("hill1.png", "hill2.png", "noise1.png", "noise2.png");
    public static final List<String> GENERATOR_BRUSHES = Arrays.asList("mountain1.png", "mountain2.png", "mountain4.png",
            "mountain5.png", "mountain6.png", "volcano2.png");
    public static final String CUSTOM_BRUSHES_DIR = "/images/brushes/";

    public static FloatMask loadBrush(String brushPath, Long seed) {
        try {
            if (Brushes.class.getResource(CUSTOM_BRUSHES_DIR + brushPath) != null) {
                BufferedImage image = ImageIO.read(Brushes.class.getResourceAsStream(CUSTOM_BRUSHES_DIR + brushPath));
                return new FloatMask(image, seed, new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            } else {
                BufferedImage image = ImageIO.read(Paths.get(brushPath).toFile());
                return new FloatMask(image, seed, new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not load brush");
        }
    }
}
