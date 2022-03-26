/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.util.Base64;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.util.Rectangle;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of a canvas that uses SVG for painting. This canvas
 * ignores the STYLE_LABEL_BACKGROUNDCOLOR and
 * STYLE_LABEL_BORDERCOLOR styles due to limitations of SVG.
 */
public class SvgCanvas extends BasicCanvas {

    private static final Logger log = Logger.getLogger(SvgCanvas.class.getName());
    /**
     * Used internally for looking up elements. Workaround for getElementById
     * not working.
     */
    private final Map<String, Element> gradients = new Hashtable<String, Element>();
    /**
     * Used internally for looking up images.
     */
    private final Map<String, Element> images = new Hashtable<String, Element>();
    /**
     * Holds the HTML document that represents the canvas.
     */
    protected Document document;
    /**
     *
     */
    protected Element defs = null;

    /**
     * Specifies if images should be embedded as base64 encoded strings.
     * Default is false.
     */
    protected boolean embedded = false;

    /**
     * Constructs a new SVG canvas for the specified dimension and scale.
     */
    public SvgCanvas() {
        this(null);
    }

    /**
     * Constructs a new SVG canvas for the specified bounds, scale and
     * background color.
     */
    public SvgCanvas(Document document) {
        setDocument(document);
    }

    /**
     *
     */
    public void appendSvgElement(Element node) {
        if (document != null) {
            document.getDocumentElement().appendChild(node);
        }
    }

    /**
     *
     */
    protected Element getDefsElement() {
        if (defs == null) {
            defs = document.createElement("defs");

            Element svgNode = document.getDocumentElement();

            if (svgNode.hasChildNodes()) {
                svgNode.insertBefore(defs, svgNode.getFirstChild());
            } else {
                svgNode.appendChild(defs);
            }
        }

        return defs;
    }

    /**
     *
     */
    public Element getGradientElement(String start, String end, String direction) {
        String id = getGradientId(start, end, direction);
        Element gradient = gradients.get(id);

        if (gradient == null) {
            gradient = createGradientElement(start, end, direction);
            gradient.setAttribute("id", "g" + (gradients.size() + 1));
            getDefsElement().appendChild(gradient);
            gradients.put(id, gradient);
        }

        return gradient;
    }

    /**
     *
     */
    public Element getGlassGradientElement() {
        String id = "glass-gradient";

        Element glassGradient = gradients.get(id);

        if (glassGradient == null) {
            glassGradient = document.createElement("linearGradient");
            glassGradient.setAttribute("x1", "0%");
            glassGradient.setAttribute("y1", "0%");
            glassGradient.setAttribute("x2", "0%");
            glassGradient.setAttribute("y2", "100%");

            Element stop1 = document.createElement("stop");
            stop1.setAttribute("offset", "0%");
            stop1.setAttribute("style", "stop-color:#ffffff;stop-opacity:0.9");
            glassGradient.appendChild(stop1);

            Element stop2 = document.createElement("stop");
            stop2.setAttribute("offset", "100%");
            stop2.setAttribute("style", "stop-color:#ffffff;stop-opacity:0.1");
            glassGradient.appendChild(stop2);

            glassGradient.setAttribute("id", "g" + (gradients.size() + 1));
            getDefsElement().appendChild(glassGradient);
            gradients.put(id, glassGradient);
        }

        return glassGradient;
    }

    /**
     *
     */
    protected Element createGradientElement(String start, String end, String direction) {
        Element gradient = document.createElement("linearGradient");
        gradient.setAttribute("x1", "0%");
        gradient.setAttribute("y1", "0%");
        gradient.setAttribute("x2", "0%");
        gradient.setAttribute("y2", "0%");

        if (direction == null || direction.equals(Constants.DIRECTION_SOUTH)) {
            gradient.setAttribute("y2", "100%");
        } else if (direction.equals(Constants.DIRECTION_EAST)) {
            gradient.setAttribute("x2", "100%");
        } else if (direction.equals(Constants.DIRECTION_NORTH)) {
            gradient.setAttribute("y1", "100%");
        } else if (direction.equals(Constants.DIRECTION_WEST)) {
            gradient.setAttribute("x1", "100%");
        }

        Element stop = document.createElement("stop");
        stop.setAttribute("offset", "0%");
        stop.setAttribute("style", "stop-color:" + start);
        gradient.appendChild(stop);

        stop = document.createElement("stop");
        stop.setAttribute("offset", "100%");
        stop.setAttribute("style", "stop-color:" + end);
        gradient.appendChild(stop);

        return gradient;
    }

    /**
     *
     */
    public String getGradientId(String start, String end, String direction) {
        // Removes illegal characters from gradient ID
        if (start.startsWith("#")) {
            start = start.substring(1);
        }

        if (end.startsWith("#")) {
            end = end.substring(1);
        }

        // Workaround for gradient IDs not working in Safari 5 / Chrome 6
        // if they contain uppercase characters
        start = start.toLowerCase();
        end = end.toLowerCase();

        String dir = null;

        if (direction == null || direction.equals(Constants.DIRECTION_SOUTH)) {
            dir = "south";
        } else if (direction.equals(Constants.DIRECTION_EAST)) {
            dir = "east";
        } else {
            String tmp = start;
            start = end;
            end = tmp;

            if (direction.equals(Constants.DIRECTION_NORTH)) {
                dir = "south";
            } else if (direction.equals(Constants.DIRECTION_WEST)) {
                dir = "east";
            }
        }

        return "gradient-" + start + "-" + end + "-" + dir;
    }

    /**
     * Returns true if the given string ends with .png, .jpg or .gif.
     */
    protected boolean isImageResource(String src) {
        return src != null && (src.toLowerCase().endsWith(".png") || src.toLowerCase().endsWith(".jpg") || src.toLowerCase().endsWith(".gif"));
    }

    /**
     *
     */
    protected InputStream getResource(String src) {
        InputStream stream = null;

        try {
            stream = new BufferedInputStream(new URL(src).openStream());
        } catch (Exception e1) {
            stream = getClass().getResourceAsStream(src);
        }

        return stream;
    }

    /**
     * @throws IOException
     */
    protected String createDataUrl(String src) throws IOException {
        String result = null;
        InputStream inputStream = isImageResource(src) ? getResource(src) : null;

        if (inputStream != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
            byte[] bytes = new byte[512];

            // Read bytes from the input stream in bytes.length-sized chunks and write
            // them into the output stream
            int readBytes;
            while ((readBytes = inputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, readBytes);
            }

            // Convert the contents of the output stream into a Data URL
            String format = "png";
            int dot = src.lastIndexOf('.');

            if (dot > 0 && dot < src.length()) {
                format = src.substring(dot + 1);
            }

            result = "data:image/" + format + ";base64," + Base64.encodeToString(outputStream.toByteArray(), false);
        }

        return result;
    }

    /**
     *
     */
    protected Element getEmbeddedImageElement(String src) {
        Element img = images.get(src);

        if (img == null) {
            img = document.createElement("svg");
            img.setAttribute("width", "100%");
            img.setAttribute("height", "100%");

            Element inner = document.createElement("image");
            inner.setAttribute("width", "100%");
            inner.setAttribute("height", "100%");

            // Store before transforming to DataURL
            images.put(src, img);

            if (!src.startsWith("data:image/")) {
                try {
                    String tmp = createDataUrl(src);

                    if (tmp != null) {
                        src = tmp;
                    }
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Failed to create image data URL", e);
                }
            }

            inner.setAttributeNS(Constants.NS_XLINK, "xlink:href", src);
            img.appendChild(inner);
            img.setAttribute("id", "i" + (images.size()));
            getDefsElement().appendChild(img);
        }

        return img;
    }

    /**
     *
     */
    protected Element createImageElement(double x, double y, double w, double h, String src, boolean aspect, boolean flipH, boolean flipV, boolean embedded) {
        Element elem = null;

        if (embedded) {
            elem = document.createElement("use");

            Element img = getEmbeddedImageElement(src);
            elem.setAttributeNS(Constants.NS_XLINK, "xlink:href", "#" + img.getAttribute("id"));
        } else {
            elem = document.createElement("image");

            elem.setAttributeNS(Constants.NS_XLINK, "xlink:href", src);
        }

        elem.setAttribute("x", String.valueOf(x));
        elem.setAttribute("y", String.valueOf(y));
        elem.setAttribute("width", String.valueOf(w));
        elem.setAttribute("height", String.valueOf(h));

        // FIXME: SVG element must be used for reference to image with
        // aspect but for images with no aspect this does not work.
        if (aspect) {
            elem.setAttribute("preserveAspectRatio", "xMidYMid");
        } else {
            elem.setAttribute("preserveAspectRatio", "none");
        }

        double sx = 1;
        double sy = 1;
        double dx = 0;
        double dy = 0;

        if (flipH) {
            sx *= -1;
            dx = -w - 2 * x;
        }

        if (flipV) {
            sy *= -1;
            dy = -h - 2 * y;
        }

        String transform = "";

        if (sx != 1 || sy != 1) {
            transform += "scale(" + sx + " " + sy + ") ";
        }

        if (dx != 0 || dy != 0) {
            transform += "translate(" + dx + " " + dy + ") ";
        }

        if (transform.length() > 0) {
            elem.setAttribute("transform", transform);
        }

        return elem;
    }

    /**
     * Returns a reference to the document that represents the canvas.
     *
     * @return Returns the document.
     */
    public Document getDocument() {
        return document;
    }

    /**
     *
     */
    public void setDocument(Document document) {
        this.document = document;
    }

    /**
     *
     */
    public boolean isEmbedded() {
        return embedded;
    }

    /**
     *
     */
    public void setEmbedded(boolean value) {
        embedded = value;
    }

    /*
     * (non-Javadoc)
     * @see com.faforever.neroxis.ngraph.canvas.ICanvas#drawCell()
     */
    public Object drawCell(CellState state) {
        Map<String, Object> style = state.getStyle();
        Element elem = null;

        if (state.getAbsolutePointCount() > 1) {
            List<Point> pts = state.getAbsolutePoints();

            // Transpose all points by cloning into a new array
            pts = Utils.translatePoints(pts, translate.getX(), translate.getY());

            // Draws the line
            elem = drawLine(pts, style);

            // Applies opacity
            float opacity = Utils.getFloat(style, Constants.STYLE_OPACITY, 100);
            float fillOpacity = Utils.getFloat(style, Constants.STYLE_FILL_OPACITY, 100);
            float strokeOpacity = Utils.getFloat(style, Constants.STYLE_STROKE_OPACITY, 100);

            if (opacity != 100 || fillOpacity != 100 || strokeOpacity != 100) {
                String fillOpac = String.valueOf(opacity * fillOpacity / 10000);
                String strokeOpac = String.valueOf(opacity * strokeOpacity / 10000);
                elem.setAttribute("fill-opacity", fillOpac);
                elem.setAttribute("stroke-opacity", strokeOpac);
            }
        } else {
            int x = (int) (state.getX() + translate.getX());
            int y = (int) (state.getY() + translate.getY());
            int w = (int) state.getWidth();
            int h = (int) state.getHeight();

            if (!Utils.getString(style, Constants.STYLE_SHAPE, "").equals(Constants.SHAPE_SWIMLANE)) {
                elem = drawShape(x, y, w, h, style);
            } else {
                int start = (int) Math.round(Utils.getInt(style, Constants.STYLE_STARTSIZE, Constants.DEFAULT_STARTSIZE) * scale);

                // Removes some styles to draw the content area
                Map<String, Object> cloned = new Hashtable<String, Object>(style);
                cloned.remove(Constants.STYLE_FILLCOLOR);
                cloned.remove(Constants.STYLE_ROUNDED);

                if (Utils.isTrue(style, Constants.STYLE_HORIZONTAL, true)) {
                    elem = drawShape(x, y, w, start, style);
                    drawShape(x, y + start, w, h - start, cloned);
                } else {
                    elem = drawShape(x, y, start, h, style);
                    drawShape(x + start, y, w - start, h, cloned);
                }
            }
        }

        return elem;
    }

    /*
     * (non-Javadoc)
     * @see com.faforever.neroxis.ngraph.canvas.ICanvas#drawLabel()
     */
    public Object drawLabel(String label, CellState state, boolean html) {
        Rectangle bounds = state.getLabelBounds();

        if (drawLabels && bounds != null) {
            int x = (int) (bounds.getX() + translate.getX());
            int y = (int) (bounds.getY() + translate.getY());
            int w = (int) bounds.getWidth();
            int h = (int) bounds.getHeight();
            Map<String, Object> style = state.getStyle();

            return drawText(label, x, y, w, h, style);
        }

        return null;
    }

    /**
     * Draws the shape specified with the STYLE_SHAPE key in the given style.
     *
     * @param x     X-coordinate of the shape.
     * @param y     Y-coordinate of the shape.
     * @param w     Width of the shape.
     * @param h     Height of the shape.
     * @param style Style of the the shape.
     */
    public Element drawShape(int x, int y, int w, int h, Map<String, Object> style) {
        String fillColor = Utils.getString(style, Constants.STYLE_FILLCOLOR, "none");
        String gradientColor = Utils.getString(style, Constants.STYLE_GRADIENTCOLOR, "none");
        String strokeColor = Utils.getString(style, Constants.STYLE_STROKECOLOR, "none");
        float strokeWidth = (float) (Utils.getFloat(style, Constants.STYLE_STROKEWIDTH, 1) * scale);
        float opacity = Utils.getFloat(style, Constants.STYLE_OPACITY, 100);
        float fillOpacity = Utils.getFloat(style, Constants.STYLE_FILL_OPACITY, 100);
        float strokeOpacity = Utils.getFloat(style, Constants.STYLE_STROKE_OPACITY, 100);

        // Draws the shape
        String shape = Utils.getString(style, Constants.STYLE_SHAPE, "");
        Element elem = null;
        Element background = null;

        if (shape.equals(Constants.SHAPE_IMAGE)) {
            String img = getImageForStyle(style);

            if (img != null) {
                // Vertical and horizontal image flipping
                boolean flipH = Utils.isTrue(style, Constants.STYLE_IMAGE_FLIPH, false);
                boolean flipV = Utils.isTrue(style, Constants.STYLE_IMAGE_FLIPV, false);

                elem = createImageElement(x, y, w, h, img, PRESERVE_IMAGE_ASPECT, flipH, flipV, isEmbedded());
            }
        } else if (shape.equals(Constants.SHAPE_LINE)) {
            String direction = Utils.getString(style, Constants.STYLE_DIRECTION, Constants.DIRECTION_EAST);
            String d = null;

            if (direction.equals(Constants.DIRECTION_EAST) || direction.equals(Constants.DIRECTION_WEST)) {
                int mid = (y + h / 2);
                d = "M " + x + " " + mid + " L " + (x + w) + " " + mid;
            } else {
                int mid = (x + w / 2);
                d = "M " + mid + " " + y + " L " + mid + " " + (y + h);
            }

            elem = document.createElement("path");
            elem.setAttribute("d", d + " Z");
        } else if (shape.equals(Constants.SHAPE_ELLIPSE)) {
            elem = document.createElement("ellipse");

            elem.setAttribute("cx", String.valueOf(x + w / 2));
            elem.setAttribute("cy", String.valueOf(y + h / 2));
            elem.setAttribute("rx", String.valueOf(w / 2));
            elem.setAttribute("ry", String.valueOf(h / 2));
        } else if (shape.equals(Constants.SHAPE_DOUBLE_ELLIPSE)) {
            elem = document.createElement("g");
            background = document.createElement("ellipse");
            background.setAttribute("cx", String.valueOf(x + w / 2));
            background.setAttribute("cy", String.valueOf(y + h / 2));
            background.setAttribute("rx", String.valueOf(w / 2));
            background.setAttribute("ry", String.valueOf(h / 2));
            elem.appendChild(background);

            int inset = (int) ((3 + strokeWidth) * scale);

            Element foreground = document.createElement("ellipse");
            foreground.setAttribute("fill", "none");
            foreground.setAttribute("stroke", strokeColor);
            foreground.setAttribute("stroke-width", String.valueOf(strokeWidth));

            foreground.setAttribute("cx", String.valueOf(x + w / 2));
            foreground.setAttribute("cy", String.valueOf(y + h / 2));
            foreground.setAttribute("rx", String.valueOf(w / 2 - inset));
            foreground.setAttribute("ry", String.valueOf(h / 2 - inset));
            elem.appendChild(foreground);
        } else if (shape.equals(Constants.SHAPE_RHOMBUS)) {
            elem = document.createElement("path");

            String d = "M " + (x + w / 2) + " " + y + " L " + (x + w) + " " + (y + h / 2) + " L " + (x + w / 2) + " " + (y + h) + " L " + x + " " + (y + h / 2);

            elem.setAttribute("d", d + " Z");
        } else if (shape.equals(Constants.SHAPE_TRIANGLE)) {
            elem = document.createElement("path");
            String direction = Utils.getString(style, Constants.STYLE_DIRECTION, "");
            String d = null;

            if (direction.equals(Constants.DIRECTION_NORTH)) {
                d = "M " + x + " " + (y + h) + " L " + (x + w / 2) + " " + y + " L " + (x + w) + " " + (y + h);
            } else if (direction.equals(Constants.DIRECTION_SOUTH)) {
                d = "M " + x + " " + y + " L " + (x + w / 2) + " " + (y + h) + " L " + (x + w) + " " + y;
            } else if (direction.equals(Constants.DIRECTION_WEST)) {
                d = "M " + (x + w) + " " + y + " L " + x + " " + (y + h / 2) + " L " + (x + w) + " " + (y + h);
            } else
            // east
            {
                d = "M " + x + " " + y + " L " + (x + w) + " " + (y + h / 2) + " L " + x + " " + (y + h);
            }

            elem.setAttribute("d", d + " Z");
        } else if (shape.equals(Constants.SHAPE_HEXAGON)) {
            elem = document.createElement("path");
            String direction = Utils.getString(style, Constants.STYLE_DIRECTION, "");
            String d = null;

            if (direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_SOUTH)) {
                d = "M " + (x + 0.5 * w) + " " + y + " L " + (x + w) + " " + (y + 0.25 * h) + " L " + (x + w) + " " + (y + 0.75 * h) + " L " + (x + 0.5 * w) + " " + (y + h) + " L " + x + " " + (y + 0.75 * h) + " L " + x + " " + (y + 0.25 * h);
            } else {
                d = "M " + (x + 0.25 * w) + " " + y + " L " + (x + 0.75 * w) + " " + y + " L " + (x + w) + " " + (y + 0.5 * h) + " L " + (x + 0.75 * w) + " " + (y + h) + " L " + (x + 0.25 * w) + " " + (y + h) + " L " + x + " " + (y + 0.5 * h);
            }

            elem.setAttribute("d", d + " Z");
        } else if (shape.equals(Constants.SHAPE_CLOUD)) {
            elem = document.createElement("path");

            String d = "M " + (x + 0.25 * w) + " " + (y + 0.25 * h) + " C " + (x + 0.05 * w) + " " + (y + 0.25 * h) + " " + x + " " + (y + 0.5 * h) + " " + (x + 0.16 * w) + " " + (y + 0.55 * h) + " C " + x + " " + (y + 0.66 * h) + " " + (x + 0.18 * w) + " " + (y + 0.9 * h) + " " + (x + 0.31 * w) + " " + (y + 0.8 * h) + " C " + (x + 0.4 * w) + " " + (y + h) + " " + (x + 0.7 * w) + " " + (y + h) + " " + (x + 0.8 * w) + " " + (y + 0.8 * h) + " C " + (x + w) + " " + (y + 0.8 * h) + " " + (x + w) + " " + (y + 0.6 * h) + " " + (x + 0.875 * w) + " " + (y + 0.5 * h) + " C " + (x + w) + " " + (y + 0.3 * h) + " " + (x + 0.8 * w) + " " + (y + 0.1 * h) + " " + (x + 0.625 * w) + " " + (y + 0.2 * h) + " C " + (x + 0.5 * w) + " " + (y + 0.05 * h) + " " + (x + 0.3 * w) + " " + (y + 0.05 * h) + " " + (x + 0.25 * w) + " " + (y + 0.25 * h);

            elem.setAttribute("d", d + " Z");
        } else if (shape.equals(Constants.SHAPE_ACTOR)) {
            elem = document.createElement("path");
            double width3 = w / 3;

            String d = " M " + x + " " + (y + h) + " C " + x + " " + (y + 3 * h / 5) + " " + x + " " + (y + 2 * h / 5) + " " + (x + w / 2) + " " + (y + 2 * h / 5) + " C " + (x + w / 2 - width3) + " " + (y + 2 * h / 5) + " " + (x + w / 2 - width3) + " " + y + " " + (x + w / 2) + " " + y + " C " + (x + w / 2 + width3) + " " + y + " " + (x + w / 2 + width3) + " " + (y + 2 * h / 5) + " " + (x + w / 2) + " " + (y + 2 * h / 5) + " C " + (x + w) + " " + (y + 2 * h / 5) + " " + (x + w) + " " + (y + 3 * h / 5) + " " + (x + w) + " " + (y + h);

            elem.setAttribute("d", d + " Z");
        } else if (shape.equals(Constants.SHAPE_CYLINDER)) {
            elem = document.createElement("g");
            background = document.createElement("path");

            double dy = Math.min(40, Math.floor(h / 5));
            String d = " M " + x + " " + (y + dy) + " C " + x + " " + (y - dy / 3) + " " + (x + w) + " " + (y - dy / 3) + " " + (x + w) + " " + (y + dy) + " L " + (x + w) + " " + (y + h - dy) + " C " + (x + w) + " " + (y + h + dy / 3) + " " + x + " " + (y + h + dy / 3) + " " + x + " " + (y + h - dy);
            background.setAttribute("d", d + " Z");
            elem.appendChild(background);

            Element foreground = document.createElement("path");
            d = "M " + x + " " + (y + dy) + " C " + x + " " + (y + 2 * dy) + " " + (x + w) + " " + (y + 2 * dy) + " " + (x + w) + " " + (y + dy);

            foreground.setAttribute("d", d);
            foreground.setAttribute("fill", "none");
            foreground.setAttribute("stroke", strokeColor);
            foreground.setAttribute("stroke-width", String.valueOf(strokeWidth));

            elem.appendChild(foreground);
        } else {
            background = document.createElement("rect");
            elem = background;

            elem.setAttribute("x", String.valueOf(x));
            elem.setAttribute("y", String.valueOf(y));
            elem.setAttribute("width", String.valueOf(w));
            elem.setAttribute("height", String.valueOf(h));

            if (Utils.isTrue(style, Constants.STYLE_ROUNDED, false)) {
                String r = String.valueOf(Math.min(w * Constants.RECTANGLE_ROUNDING_FACTOR, h * Constants.RECTANGLE_ROUNDING_FACTOR));

                elem.setAttribute("rx", r);
                elem.setAttribute("ry", r);
            }

            // Paints the label image
            if (shape.equals(Constants.SHAPE_LABEL)) {
                String img = getImageForStyle(style);

                if (img != null) {
                    String imgAlign = Utils.getString(style, Constants.STYLE_IMAGE_ALIGN, Constants.ALIGN_LEFT);
                    String imgValign = Utils.getString(style, Constants.STYLE_IMAGE_VERTICAL_ALIGN, Constants.ALIGN_MIDDLE);
                    int imgWidth = (int) (Utils.getInt(style, Constants.STYLE_IMAGE_WIDTH, Constants.DEFAULT_IMAGESIZE) * scale);
                    int imgHeight = (int) (Utils.getInt(style, Constants.STYLE_IMAGE_HEIGHT, Constants.DEFAULT_IMAGESIZE) * scale);
                    int spacing = (int) (Utils.getInt(style, Constants.STYLE_SPACING, 2) * scale);

                    Rectangle imageBounds = new Rectangle(x, y, w, h);

                    if (imgAlign.equals(Constants.ALIGN_CENTER)) {
                        imageBounds.setX(imageBounds.getX() + (imageBounds.getWidth() - imgWidth) / 2);
                    } else if (imgAlign.equals(Constants.ALIGN_RIGHT)) {
                        imageBounds.setX(imageBounds.getX() + imageBounds.getWidth() - imgWidth - spacing - 2);
                    } else
                    // LEFT
                    {
                        imageBounds.setX(imageBounds.getX() + spacing + 4);
                    }

                    if (imgValign.equals(Constants.ALIGN_TOP)) {
                        imageBounds.setY(imageBounds.getY() + spacing);
                    } else if (imgValign.equals(Constants.ALIGN_BOTTOM)) {
                        imageBounds.setY(imageBounds.getY() + imageBounds.getHeight() - imgHeight - spacing);
                    } else
                    // MIDDLE
                    {
                        imageBounds.setY(imageBounds.getY() + (imageBounds.getHeight() - imgHeight) / 2);
                    }

                    imageBounds.setWidth(imgWidth);
                    imageBounds.setHeight(imgHeight);

                    elem = document.createElement("g");
                    elem.appendChild(background);

                    Element imageElement = createImageElement(imageBounds.getX(), imageBounds.getY(), imageBounds.getWidth(), imageBounds.getHeight(), img, false, false, false, isEmbedded());

                    if (opacity != 100 || fillOpacity != 100) {
                        String value = String.valueOf(opacity * fillOpacity / 10000);
                        imageElement.setAttribute("opacity", value);
                    }

                    elem.appendChild(imageElement);
                }

                // Paints the glass effect
                if (Utils.isTrue(style, Constants.STYLE_GLASS, false)) {
                    double size = 0.4;

                    // TODO: Mask with rectangle or rounded rectangle of label
                    // Creates glass overlay
                    Element glassOverlay = document.createElement("path");

                    // LATER: Not sure what the behaviour is for mutiple SVG elements in page.
                    // Probably its possible that this points to an element in another SVG
                    // node which when removed will result in an undefined background.
                    glassOverlay.setAttribute("fill", "url(#" + getGlassGradientElement().getAttribute("id") + ")");

                    String d = "m " + (x - strokeWidth) + "," + (y - strokeWidth) + " L " + (x - strokeWidth) + "," + (y + h * size) + " Q " + (x + w * 0.5) + "," + (y + h * 0.7) + " " + (x + w + strokeWidth) + "," + (y + h * size) + " L " + (x + w + strokeWidth) + "," + (y - strokeWidth) + " z";
                    glassOverlay.setAttribute("stroke-width", String.valueOf(strokeWidth / 2));
                    glassOverlay.setAttribute("d", d);
                    elem.appendChild(glassOverlay);
                }
            }
        }

        double rotation = Utils.getDouble(style, Constants.STYLE_ROTATION);
        int cx = x + w / 2;
        int cy = y + h / 2;

        Element bg = background;

        if (bg == null) {
            bg = elem;
        }

        if (!bg.getNodeName().equalsIgnoreCase("use") && !bg.getNodeName().equalsIgnoreCase("image")) {
            if (!fillColor.equalsIgnoreCase("none") && !gradientColor.equalsIgnoreCase("none")) {
                String direction = Utils.getString(style, Constants.STYLE_GRADIENT_DIRECTION);
                Element gradient = getGradientElement(fillColor, gradientColor, direction);

                if (gradient != null) {
                    bg.setAttribute("fill", "url(#" + gradient.getAttribute("id") + ")");
                }
            } else {
                bg.setAttribute("fill", fillColor);
            }

            bg.setAttribute("stroke", strokeColor);
            bg.setAttribute("stroke-width", String.valueOf(strokeWidth));

            // Adds the shadow element
            Element shadowElement = null;

            if (Utils.isTrue(style, Constants.STYLE_SHADOW, false) && !fillColor.equals("none")) {
                shadowElement = (Element) bg.cloneNode(true);

                shadowElement.setAttribute("transform", Constants.SVG_SHADOWTRANSFORM);
                shadowElement.setAttribute("fill", Constants.W3C_SHADOWCOLOR);
                shadowElement.setAttribute("stroke", Constants.W3C_SHADOWCOLOR);
                shadowElement.setAttribute("stroke-width", String.valueOf(strokeWidth));

                if (rotation != 0) {
                    shadowElement.setAttribute("transform", "rotate(" + rotation + "," + cx + "," + cy + ") " + Constants.SVG_SHADOWTRANSFORM);
                }

                if (opacity != 100) {
                    String value = String.valueOf(opacity / 100);
                    shadowElement.setAttribute("fill-opacity", value);
                    shadowElement.setAttribute("stroke-opacity", value);
                }

                appendSvgElement(shadowElement);
            }
        }

        if (rotation != 0) {
            elem.setAttribute("transform", elem.getAttribute("transform") + " rotate(" + rotation + "," + cx + "," + cy + ")");

        }

        if (opacity != 100 || fillOpacity != 100 || strokeOpacity != 100) {
            String fillValue = String.valueOf(opacity * fillOpacity / 10000);
            String strokeValue = String.valueOf(opacity * strokeOpacity / 10000);
            elem.setAttribute("fill-opacity", fillValue);
            elem.setAttribute("stroke-opacity", strokeValue);
        }

        if (Utils.isTrue(style, Constants.STYLE_DASHED)) {
            String pattern = Utils.getString(style, Constants.STYLE_DASH_PATTERN, "3, 3");
            elem.setAttribute("stroke-dasharray", pattern);
        }

        appendSvgElement(elem);

        return elem;
    }

    /**
     * Draws the given lines as segments between all points of the given list
     * of Points.
     *
     * @param pts   List of points that define the line.
     * @param style Style to be used for painting the line.
     */
    public Element drawLine(List<Point> pts, Map<String, Object> style) {
        Element group = document.createElement("g");
        Element path = document.createElement("path");

        boolean rounded = Utils.isTrue(style, Constants.STYLE_ROUNDED, false);
        String strokeColor = Utils.getString(style, Constants.STYLE_STROKECOLOR);
        float tmpStroke = (Utils.getFloat(style, Constants.STYLE_STROKEWIDTH, 1));
        float strokeWidth = (float) (tmpStroke * scale);

        if (strokeColor != null && strokeWidth > 0) {
            // Draws the start marker
            Object marker = style.get(Constants.STYLE_STARTARROW);

            Point pt = pts.get(1);
            Point p0 = pts.get(0);
            Point offset = null;

            if (marker != null) {
                float size = (Utils.getFloat(style, Constants.STYLE_STARTSIZE, Constants.DEFAULT_MARKERSIZE));
                offset = drawMarker(group, marker, pt, p0, size, tmpStroke, strokeColor);
            } else {
                double dx = pt.getX() - p0.getX();
                double dy = pt.getY() - p0.getY();

                double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                double nx = dx * strokeWidth / dist;
                double ny = dy * strokeWidth / dist;

                offset = new Point(nx / 2, ny / 2);
            }

            // Applies offset to the point
            if (offset != null) {
                p0 = (Point) p0.clone();
                p0.setX(p0.getX() + offset.getX());
                p0.setY(p0.getY() + offset.getY());

                offset = null;
            }

            // Draws the end marker
            marker = style.get(Constants.STYLE_ENDARROW);

            pt = pts.get(pts.size() - 2);
            Point pe = pts.get(pts.size() - 1);

            if (marker != null) {
                float size = (Utils.getFloat(style, Constants.STYLE_ENDSIZE, Constants.DEFAULT_MARKERSIZE));
                offset = drawMarker(group, marker, pt, pe, size, tmpStroke, strokeColor);
            } else {
                double dx = pt.getX() - p0.getX();
                double dy = pt.getY() - p0.getY();

                double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                double nx = dx * strokeWidth / dist;
                double ny = dy * strokeWidth / dist;

                offset = new Point(nx / 2, ny / 2);
            }

            // Applies offset to the point
            if (offset != null) {
                pe = (Point) pe.clone();
                pe.setX(pe.getX() + offset.getX());
                pe.setY(pe.getY() + offset.getY());

                offset = null;
            }

            // Draws the line segments
            double arcSize = Constants.LINE_ARCSIZE * scale;
            pt = p0;
            String d = "M " + pt.getX() + " " + pt.getY();

            for (int i = 1; i < pts.size() - 1; i++) {
                Point tmp = pts.get(i);
                double dx = pt.getX() - tmp.getX();
                double dy = pt.getY() - tmp.getY();

                if ((rounded && i < pts.size() - 1) && (dx != 0 || dy != 0)) {
                    // Draws a line from the last point to the current
                    // point with a spacing of size off the current point
                    // into direction of the last point
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double nx1 = dx * Math.min(arcSize, dist / 2) / dist;
                    double ny1 = dy * Math.min(arcSize, dist / 2) / dist;

                    double x1 = tmp.getX() + nx1;
                    double y1 = tmp.getY() + ny1;
                    d += " L " + x1 + " " + y1;

                    // Draws a curve from the last point to the current
                    // point with a spacing of size off the current point
                    // into direction of the next point
                    Point next = pts.get(i + 1);
                    dx = next.getX() - tmp.getX();
                    dy = next.getY() - tmp.getY();

                    dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                    double nx2 = dx * Math.min(arcSize, dist / 2) / dist;
                    double ny2 = dy * Math.min(arcSize, dist / 2) / dist;

                    double x2 = tmp.getX() + nx2;
                    double y2 = tmp.getY() + ny2;

                    d += " Q " + tmp.getX() + " " + tmp.getY() + " " + x2 + " " + y2;
                    tmp = new Point(x2, y2);
                } else {
                    d += " L " + tmp.getX() + " " + tmp.getY();
                }

                pt = tmp;
            }

            d += " L " + pe.getX() + " " + pe.getY();

            path.setAttribute("d", d);
            path.setAttribute("stroke", strokeColor);
            path.setAttribute("fill", "none");
            path.setAttribute("stroke-width", String.valueOf(strokeWidth));

            if (Utils.isTrue(style, Constants.STYLE_DASHED)) {
                String pattern = Utils.getString(style, Constants.STYLE_DASH_PATTERN, "3, 3");
                path.setAttribute("stroke-dasharray", pattern);
            }

            group.appendChild(path);
            appendSvgElement(group);
        }

        return group;
    }

    /**
     * Draws the specified marker as a child path in the given parent.
     */
    public Point drawMarker(Element parent, Object type, Point p0, Point pe, float size, float strokeWidth, String color) {
        Point offset = null;

        // Computes the norm and the inverse norm
        double dx = pe.getX() - p0.getX();
        double dy = pe.getY() - p0.getY();

        double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        double absSize = size * scale;
        double nx = dx * absSize / dist;
        double ny = dy * absSize / dist;

        pe = (Point) pe.clone();
        pe.setX(pe.getX() - nx * strokeWidth / (2 * size));
        pe.setY(pe.getY() - ny * strokeWidth / (2 * size));

        nx *= 0.5 + strokeWidth / 2;
        ny *= 0.5 + strokeWidth / 2;

        Element path = document.createElement("path");
        path.setAttribute("stroke-width", String.valueOf(strokeWidth * scale));
        path.setAttribute("stroke", color);
        path.setAttribute("fill", color);

        String d = null;

        if (type.equals(Constants.ARROW_CLASSIC) || type.equals(Constants.ARROW_BLOCK)) {
            d = "M " + pe.getX() + " " + pe.getY() + " L " + (pe.getX() - nx - ny / 2) + " " + (pe.getY() - ny + nx / 2) + ((!type.equals(Constants.ARROW_CLASSIC)) ? "" : " L " + (pe.getX() - nx * 3 / 4) + " " + (pe.getY() - ny * 3 / 4)) + " L " + (pe.getX() + ny / 2 - nx) + " " + (pe.getY() - ny - nx / 2) + " z";
        } else if (type.equals(Constants.ARROW_OPEN)) {
            nx *= 1.2;
            ny *= 1.2;

            d = "M " + (pe.getX() - nx - ny / 2) + " " + (pe.getY() - ny + nx / 2) + " L " + (pe.getX() - nx / 6) + " " + (pe.getY() - ny / 6) + " L " + (pe.getX() + ny / 2 - nx) + " " + (pe.getY() - ny - nx / 2) + " M " + pe.getX() + " " + pe.getY();
            path.setAttribute("fill", "none");
        } else if (type.equals(Constants.ARROW_OVAL)) {
            nx *= 1.2;
            ny *= 1.2;
            absSize *= 1.2;

            d = "M " + (pe.getX() - ny / 2) + " " + (pe.getY() + nx / 2) + " a " + (absSize / 2) + " " + (absSize / 2) + " 0  1,1 " + (nx / 8) + " " + (ny / 8) + " z";
        } else if (type.equals(Constants.ARROW_DIAMOND)) {
            d = "M " + (pe.getX() + nx / 2) + " " + (pe.getY() + ny / 2) + " L " + (pe.getX() - ny / 2) + " " + (pe.getY() + nx / 2) + " L " + (pe.getX() - nx / 2) + " " + (pe.getY() - ny / 2) + " L " + (pe.getX() + ny / 2) + " " + (pe.getY() - nx / 2) + " z";
        }

        if (d != null) {
            path.setAttribute("d", d);
            parent.appendChild(path);
        }

        return offset;
    }

    /**
     * Draws the specified text either using drawHtmlString or using drawString.
     *
     * @param text  Text to be painted.
     * @param x     X-coordinate of the text.
     * @param y     Y-coordinate of the text.
     * @param w     Width of the text.
     * @param h     Height of the text.
     * @param style Style to be used for painting the text.
     */
    public Object drawText(String text, int x, int y, int w, int h, Map<String, Object> style) {
        Element elem = null;
        String fontColor = Utils.getString(style, Constants.STYLE_FONTCOLOR, "black");
        String fontFamily = Utils.getString(style, Constants.STYLE_FONTFAMILY, Constants.DEFAULT_FONTFAMILIES);
        int fontSize = (int) (Utils.getInt(style, Constants.STYLE_FONTSIZE, Constants.DEFAULT_FONTSIZE) * scale);

        if (text != null && text.length() > 0) {
            float strokeWidth = (float) (Utils.getFloat(style, Constants.STYLE_STROKEWIDTH, 1) * scale);

            // Applies the opacity
            float opacity = Utils.getFloat(style, Constants.STYLE_TEXT_OPACITY, 100);

            // Draws the label background and border
            String bg = Utils.getString(style, Constants.STYLE_LABEL_BACKGROUNDCOLOR);
            String border = Utils.getString(style, Constants.STYLE_LABEL_BORDERCOLOR);

            String transform = null;

            if (!Utils.isTrue(style, Constants.STYLE_HORIZONTAL, true)) {
                double cx = x + w / 2;
                double cy = y + h / 2;
                transform = "rotate(270 " + cx + " " + cy + ")";
            }

            if (bg != null || border != null) {
                Element background = document.createElement("rect");

                background.setAttribute("x", String.valueOf(x));
                background.setAttribute("y", String.valueOf(y));
                background.setAttribute("width", String.valueOf(w));
                background.setAttribute("height", String.valueOf(h));

                if (bg != null) {
                    background.setAttribute("fill", bg);
                } else {
                    background.setAttribute("fill", "none");
                }

                if (border != null) {
                    background.setAttribute("stroke", border);
                } else {
                    background.setAttribute("stroke", "none");
                }

                background.setAttribute("stroke-width", String.valueOf(strokeWidth));

                if (opacity != 100) {
                    String value = String.valueOf(opacity / 100);
                    background.setAttribute("fill-opacity", value);
                    background.setAttribute("stroke-opacity", value);
                }

                if (transform != null) {
                    background.setAttribute("transform", transform);
                }

                appendSvgElement(background);
            }

            elem = document.createElement("text");

            int fontStyle = Utils.getInt(style, Constants.STYLE_FONTSTYLE);
            String weight = ((fontStyle & Constants.FONT_BOLD) == Constants.FONT_BOLD) ? "bold" : "normal";
            elem.setAttribute("font-weight", weight);

            String txtDecor = "";

            if ((fontStyle & Constants.FONT_UNDERLINE) == Constants.FONT_UNDERLINE) {
                txtDecor = "underline";
            }

            if ((fontStyle & Constants.FONT_STRIKETHROUGH) == Constants.FONT_STRIKETHROUGH) {
                txtDecor += " line-through";
            }

            elem.setAttribute("font-decoration", txtDecor.length() > 0 ? txtDecor : "none");

            if ((fontStyle & Constants.FONT_ITALIC) == Constants.FONT_ITALIC) {
                elem.setAttribute("font-style", "italic");
            }

            elem.setAttribute("font-size", String.valueOf(fontSize));
            elem.setAttribute("font-family", fontFamily);
            elem.setAttribute("fill", fontColor);

            if (opacity != 100) {
                String value = String.valueOf(opacity / 100);
                elem.setAttribute("fill-opacity", value);
                elem.setAttribute("stroke-opacity", value);
            }

            int swingFontStyle = ((fontStyle & Constants.FONT_BOLD) == Constants.FONT_BOLD) ? Font.BOLD : Font.PLAIN;
            swingFontStyle += ((fontStyle & Constants.FONT_ITALIC) == Constants.FONT_ITALIC) ? Font.ITALIC : Font.PLAIN;

            String[] lines = text.split("\n");
            y += fontSize + (h - lines.length * (fontSize + Constants.LINESPACING)) / 2 - 2;

            String align = Utils.getString(style, Constants.STYLE_ALIGN, Constants.ALIGN_CENTER);
            String anchor = "start";

            if (align.equals(Constants.ALIGN_RIGHT)) {
                anchor = "end";
                x += w - Constants.LABEL_INSET * scale;
            } else if (align.equals(Constants.ALIGN_CENTER)) {
                anchor = "middle";
                x += w / 2;
            } else {
                x += Constants.LABEL_INSET * scale;
            }

            elem.setAttribute("text-anchor", anchor);

            for (int i = 0; i < lines.length; i++) {
                Element tspan = document.createElement("tspan");

                tspan.setAttribute("x", String.valueOf(x));
                tspan.setAttribute("y", String.valueOf(y));

                tspan.appendChild(document.createTextNode(lines[i]));
                elem.appendChild(tspan);

                y += fontSize + Constants.LINESPACING;
            }

            if (transform != null) {
                elem.setAttribute("transform", transform);
            }

            appendSvgElement(elem);
        }

        return elem;
    }

}
