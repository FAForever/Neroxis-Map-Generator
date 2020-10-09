package util;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
        if (imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
            return false;
        }

        int width = imgA.getWidth();
        int height = imgA.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static BufferedImage readImage(String resource) throws IOException {
        InputStream inputStream = ImageUtils.class.getResourceAsStream(resource);
        return ImageIO.read(inputStream);
    }

    public static BufferedImage scaleImage(BufferedImage image, int width, int height) {
        width = StrictMath.max(width, 1);
        height = StrictMath.max(height, 1);
        BufferedImage imageScaled = new BufferedImage(width, height, image.getType());
        AffineTransform at = new AffineTransform();
        at.scale((double) width / image.getWidth(), (double) height / image.getHeight());
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        imageScaled = scaleOp.filter(image, imageScaled);

        return imageScaled;
    }

}
