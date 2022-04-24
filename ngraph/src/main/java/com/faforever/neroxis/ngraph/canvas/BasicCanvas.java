package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BasicCanvas implements ICanvas {

    /**
     * Specifies if image aspect should be preserved in drawImage. Default is true.
     */
    public static boolean PRESERVE_IMAGE_ASPECT = true;
    /**
     * Defines the default value for the imageBasePath in all GDI canvases.
     * Default is an empty string.
     */
    public static String DEFAULT_IMAGEBASEPATH = "";
    /**
     * Defines the base path for images with relative paths. Trailing slash
     * is required. Default value is DEFAULT_IMAGEBASEPATH.
     */
    protected String imageBasePath = DEFAULT_IMAGEBASEPATH;
    /**
     * Specifies the current translation. Default is (0,0).
     */
    protected PointDouble translate = new PointDouble();
    /**
     * Specifies the current scale. Default is 1.
     */
    protected double scale = 1;
    /**
     * Specifies whether labels should be painted. Default is true.
     */
    protected boolean drawLabels = true;
    /**
     * Cache for images.
     */
    protected HashMap<String, BufferedImage> imageCache = new HashMap<>();

    /**
     * Sets the current translate.
     */
    @Override
    public void setTranslate(double dx, double dy) {
        translate = new PointDouble(dx, dy);
    }

    /**
     * Returns an image instance for the given URL. If the URL has
     * been loaded before than an instance of the same instance is
     * returned as in the previous call.
     */
    public BufferedImage loadImage(String image) {
        BufferedImage img = imageCache.get(image);

        if (img == null) {
            img = Utils.loadImage(image);

            if (img != null) {
                imageCache.put(image, img);
            }
        }

        return img;
    }

    public void flushImageCache() {
        imageCache.clear();
    }

    /**
     * Gets the image path from the given style. If the path is relative (does
     * not start with a slash) then it is appended to the imageBasePath.
     */
    public String getImageForStyle(Style style) {
        String filename = style.getImage().getImage();
        if (filename != null && !filename.startsWith("/") && !filename.startsWith("file:/")) {
            filename = imageBasePath + filename;
        }
        return filename;
    }
}
