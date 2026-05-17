package com.faforever.neroxis.brushes;

import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.ResourceUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Brushes {
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
    public static final List<String> CLEAN_MOUNTAIN_BRUSHES = Arrays.asList("mountain4.png",
                                                                       "mountain5.png", "mountain6.png",
                                                                       "mountain7.png", "noise2.png");

    public static final String CUSTOM_BRUSHES_DIR = "/images/brushes/";

    public static FloatMask loadBrush(String brushPath, Long seed) {
        try {
            BufferedImage image;
            InputStream inputStream;
            if ((inputStream = ResourceUtil.getResourceAsStream(CUSTOM_BRUSHES_DIR + brushPath))
                != null) {
                image = ImageIO.read(inputStream);
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
