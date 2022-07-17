package com.faforever.neroxis.brushes;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import lombok.Data;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Data
public strictfp class Brushes {
    public static final List<String> MOUNTAIN_BRUSHES = Arrays.asList("mountain1.png", "mountain2.png", "mountain3.png",
            "mountain4.png", "mountain5.png", "mountain6.png",
            "mountain7.png", "mountain8.png", "mountain9.png",
            "volcano2.png", "hill1.png", "hill2.png",
            "noise1.png", "noise2.png", "hawaii1.png",
            "hawaii2.png", "volcano2.png");
    public static final List<String> HILL_BRUSHES = Arrays.asList("hill1.png", "hill2.png", "noise1.png", "noise2.png");
    public static final List<String> GENERATOR_BRUSHES = Arrays.asList("mountain1.png", "mountain2.png",
            "mountain4.png", "mountain5.png",
            "mountain6.png", "volcano2.png");
    public static final String CUSTOM_BRUSHES_DIR = "/images/brushes/";

    public static FloatMask loadBrush(String brushPath, Long seed) {
        try {
            BufferedImage image;
            if (Brushes.class.getResource(CUSTOM_BRUSHES_DIR + brushPath) != null) {
                image = ImageIO.read(
                        Objects.requireNonNull(Brushes.class.getResourceAsStream(CUSTOM_BRUSHES_DIR + brushPath)));
            } else {
                image = ImageIO.read(Paths.get(brushPath).toFile());
            }
            return new FloatMask(image, seed, new SymmetrySettings(Symmetry.NONE, Symmetry.NONE, Symmetry.NONE), 1f,
                    brushPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(String.format("Could not load brush: %s", brushPath));
        }
    }
}
