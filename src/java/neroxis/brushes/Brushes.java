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
            "hawaii1.png", "hawaii2.png", "island.png", "volcano2.png", "Brush1.png", "Brush2.png", "Brush3.png", "Brush4.png", "Brush5.png",
            "Brush6.png", "Brush7.png", "Brush8.png", "Brush9.png", "Brush10.png", "Brush11.png", "Brush12.png", "Brush13.png", "Brush14.png",
            "Brush15.png", "Brush16.png", "Brush17.png", "Brush18.png", "Brush19.png", "Brush20.png", "Brush21.png", "Brush22.png", "Brush23.png",
            "Brush24.png", "Brush25.png", "Brush26.png", "Brush27.png", "Brush28.png", "Brush29.png", "Brush30.png", "Brush31.png", "Brush32.png",
            "Brush33.png", "Brush34.png", "Brush35.png", "Brush36.png", "Brush37.png");
    public static final List<String> HILL_BRUSHES = Arrays.asList("hill1.png", "hill2.png", "noise1.png", "noise2.png", "Brush1.png", "Brush2.png", "Brush3.png", "Brush16.png",
            "Brush21.png", "Brush22.png", "Brush23.png", "Brush24.png", "Brush25.png", "Brush27.png", "Brush29.png", "Brush30.png", "Brush31.png",
            "Brush32.png", "Brush33.png", "Brush35.png", "Brush36.png");
    public static final List<String> GENERATOR_BRUSHES = Arrays.asList("mountain1.png", "mountain2.png", "mountain4.png",
            "mountain5.png", "mountain6.png", "volcano2.png", "Brush1.png", "Brush2.png", "Brush3.png", "Brush4.png", "Brush5.png",
            "Brush6.png", "Brush7.png", "Brush8.png", "Brush9.png", "Brush10.png", "Brush11.png", "Brush12.png", "Brush13.png", "Brush14.png",
            "Brush15.png", "Brush16.png", "Brush17.png", "Brush18.png", "Brush19.png", "Brush20.png", "Brush21.png", "Brush22.png", "Brush23.png",
            "Brush24.png", "Brush25.png", "Brush26.png", "Brush27.png", "Brush28.png", "Brush29.png", "Brush30.png", "Brush31.png", "Brush32.png",
            "Brush33.png", "Brush34.png", "Brush35.png", "Brush36.png", "Brush37.png");
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
