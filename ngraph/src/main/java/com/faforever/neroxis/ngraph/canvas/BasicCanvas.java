package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.util.Utils;

import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.util.Map;

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
    protected Point translate = new Point();

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
    protected Hashtable<String, BufferedImage> imageCache = new Hashtable<String, BufferedImage>();

    /**
     * Sets the current translate.
     */
    public void setTranslate(double dx, double dy) {
        translate = new Point(dx, dy);
    }

    /**
     * Returns the current translate.
     */
    public Point getTranslate() {
        return translate;
    }

    /**
     *
     */
    public double getScale() {
        return scale;
    }

    /**
     *
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     *
     */
    public String getImageBasePath() {
        return imageBasePath;
    }

    /**
     *
     */
    public void setImageBasePath(String imageBasePath) {
        this.imageBasePath = imageBasePath;
    }

    /**
     *
     */
    public boolean isDrawLabels() {
        return drawLabels;
    }

    /**
     *
     */
    public void setDrawLabels(boolean drawLabels) {
        this.drawLabels = drawLabels;
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

    /**
     *
     */
    public void flushImageCache() {
        imageCache.clear();
    }

    /**
     * Gets the image path from the given style. If the path is relative (does
     * not start with a slash) then it is appended to the imageBasePath.
     */
    public String getImageForStyle(Map<String, Object> style) {
        String filename = Utils.getString(style, Constants.STYLE_IMAGE);

        if (filename != null && !filename.startsWith("/") && !filename.startsWith("file:/")) {
            filename = imageBasePath + filename;
        }

        return filename;
    }

}
