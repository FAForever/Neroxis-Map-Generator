package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.style.util.Direction;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Utils;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.CellRendererPane;
import lombok.Setter;

/**
 * Used for exporting images. To render to an image from a given XML string,
 * graph size and background color, the following code is used:
 *
 * <code>
 * BufferedImage image = Utils.createBufferedImage(width, height, background);
 * Graphics2D g2 = image.createGraphics();
 * Utils.setAntiAlias(g2, true, true);
 * XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
 * reader.setContentHandler(new SaxOutputHandler(new GraphicsCanvas2D(g2)));
 * reader.parse(new InputSource(new StringReader(xml)));
 * </code>
 * <p>
 * Text rendering is available for plain text
 */
@Setter
public class GraphicsCanvas2D implements ICanvas2D {

    private static final Logger log = Logger.getLogger(GraphicsCanvas2D.class.getName());
    /**
     * Specifies the image scaling quality. Default is Image.SCALE_SMOOTH.
     * See {@link #scaleImage(Image, int, int)}
     */
    public static int IMAGE_SCALING = Image.SCALE_SMOOTH;
    /**
     * Unit to be used for HTML labels. Default is "pt". If you units within
     * HTML labels are used, this should match those units to produce a
     * consistent output. If the value is "px", then HTML_SCALE should be
     * changed the match the ratio between px units for rendering HTML and
     * the units used for rendering other graphics elements. This value is
     * 0.6 on Linux and 0.75 on all other platforms.
     */
    public static String HTML_UNIT = "pt";
    /**
     * Specifies the size of the cache used to store parsed colors
     */
    public static int COLOR_CACHE_SIZE = 100;
    /**
     * Reference to the graphics instance for painting.
     */
    protected Graphics2D graphics;
    /**
     * Specifies if text output should be rendered. Default is true.
     */
    protected boolean textEnabled = true;
    /**
     * Represents the current state of the canvas.
     */
    protected transient CanvasState state = new CanvasState();
    /**
     * Stack of states for save/restore.
     */
    protected transient Stack<CanvasState> stack = new Stack<>();
    /**
     * Holds the current path.
     */
    protected transient GeneralPath currentPath;
    /**
     * Optional renderer pane to be used for HTML label rendering.
     */
    protected CellRendererPane rendererPane;
    /**
     * Font caching.
     */
    protected transient Font lastFont = null;
    /**
     * Font caching.
     */
    protected transient int lastFontStyle = 0;
    /**
     * Font caching.
     */
    protected transient int lastFontSize = 0;
    /**
     * Font caching.
     */
    protected transient String lastFontFamily = "";
    /**
     * Stroke caching.
     */
    protected transient Stroke lastStroke = null;
    /**
     * Stroke caching.
     */
    protected transient float lastStrokeWidth = 0;
    /**
     * Stroke caching.
     */
    protected transient int lastCap = 0;
    /**
     * Stroke caching.
     */
    protected transient int lastJoin = 0;
    /**
     * Stroke caching.
     */
    protected transient float lastMiterLimit = 0;
    /**
     * Stroke caching.
     */
    protected transient boolean lastDashed = false;
    /**
     * Stroke caching.
     */
    protected transient Object lastDashPattern = "";
    /**
     * Caches parsed colors.
     */
    protected transient LinkedHashMap<String, Color> colorCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Color> eldest) {
            return size() > COLOR_CACHE_SIZE;
        }
    };

    /**
     * Constructs a new graphics export canvas.
     */
    public GraphicsCanvas2D(Graphics2D g) {
        setGraphics(g);
        state.g = g;
        // Initializes the cell renderer pane for drawing HTML markup
        try {
            rendererPane = new CellRendererPane();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to initialize renderer pane", e);
        }
    }

    /**
     * Returns the graphics instance.
     */
    public Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * Sets the graphics instance.
     */
    public void setGraphics(Graphics2D value) {
        graphics = value;
    }

    /**
     * Returns true if text should be rendered.
     */
    public boolean isTextEnabled() {
        return textEnabled;
    }

    /**
     * Disables or enables text rendering.
     */
    public void setTextEnabled(boolean value) {
        textEnabled = value;
    }

    /**
     * Saves the current canvas state.
     */
    @Override
    public void save() {
        stack.push(state);
        state = cloneState(state);
        state.g = (Graphics2D) state.g.create();
    }

    /**
     * Restores the last canvas state.
     */
    @Override
    public void restore() {
        state.g.dispose();
        state = stack.pop();
    }

    @Override
    public void scale(double value) {
        // This implementation uses custom scale/translate and built-in rotation
        state.scale = state.scale * value;
    }

    @Override
    public void translate(double dx, double dy) {
        // This implementation uses custom scale/translate and built-in rotation
        state.dx += dx;
        state.dy += dy;
    }

    @Override
    public void rotate(double theta, boolean flipH, boolean flipV, double cx, double cy) {
        cx += state.dx;
        cy += state.dy;
        cx *= state.scale;
        cy *= state.scale;
        state.g.rotate(Math.toRadians(theta), cx, cy);
        // This implementation uses custom scale/translate and built-in rotation
        // Rotation state is part of the AffineTransform in state.transform
        if (flipH && flipV) {
            theta += 180;
        } else if (flipH ^ flipV) {
            double tx = (flipH) ? cx : 0;
            int sx = (flipH) ? -1 : 1;
            double ty = (flipV) ? cy : 0;
            int sy = (flipV) ? -1 : 1;
            state.g.translate(tx, ty);
            state.g.scale(sx, sy);
            state.g.translate(-tx, -ty);
        }
        state.theta = theta;
        state.rotationCx = cx;
        state.rotationCy = cy;
        state.flipH = flipH;
        state.flipV = flipV;
    }

    @Override
    public void setStrokeWidth(double value) {
        // Lazy and cached instantiation strategy for all stroke properties
        if (value != state.strokeWidth) {
            state.strokeWidth = value;
        }
    }

    /**
     * Caches color conversion as it is expensive.
     */
    @Override
    public void setStrokeColor(Color value) {
        // Lazy and cached instantiation strategy for all stroke properties
        if (state.strokeColor == null || !state.strokeColor.equals(value)) {
            state.strokeColor = value;
        }
    }

    @Override
    public void setDashed(boolean value) {
        this.setDashed(value, state.fixDash);
    }

    @Override
    public void setDashed(boolean value, boolean fixDash) {
        // Lazy and cached instantiation strategy for all stroke properties
        state.dashed = value;
        state.fixDash = fixDash;
    }

    @Override
    public void setDashPattern(String value) {
        if (value != null && value.length() > 0) {
            state.dashPattern = Utils.parseDashPattern(value);
        }
    }

    @Override
    public void setLineCap(String value) {
        if (!state.lineCap.equals(value)) {
            state.lineCap = value;
        }
    }

    @Override
    public void setLineJoin(String value) {
        if (!state.lineJoin.equals(value)) {
            state.lineJoin = value;
        }
    }

    @Override
    public void setMiterLimit(double value) {
        if (value != state.miterLimit) {
            state.miterLimit = value;
        }
    }

    @Override
    public void setFontSize(double value) {
        if (value != state.fontSize) {
            state.fontSize = value;
        }
    }

    @Override
    public void setFontColor(Color value) {
        if (state.fontColor == null || !state.fontColor.equals(value)) {
            state.fontColor = value;
        }
    }

    @Override
    public void setFontFamily(String value) {
        if (!state.fontFamily.equals(value)) {
            state.fontFamily = value;
        }
    }

    @Override
    public void setFontStyle(int value) {
        if (value != state.fontStyle) {
            state.fontStyle = value;
        }
    }

    @Override
    public void setFontBackgroundColor(Color value) {
        if (state.fontBackgroundColor == null || !state.fontBackgroundColor.equals(value)) {
            state.fontBackgroundColor = null;
        }
    }

    @Override
    public void setFontBorderColor(Color value) {
        if (state.fontBorderColor == null || !state.fontBorderColor.equals(value)) {
            state.fontBorderColor = null;
        }
    }

    @Override
    public void setAlpha(double value) {
        if (state.alpha != value) {
            state.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (value)));
            state.alpha = value;
        }
    }

    @Override
    public void setFillColor(Color value) {
        if (state.fillColor == null || !state.fillColor.equals(value)) {
            state.fillColor = value;
            // Setting fill color resets gradient paint
            state.gradientPaint = null;
        }
    }

    @Override
    public void setGradient(Color color1, Color color2, double x, double y, double w, double h, Direction direction,
                            double alpha1, double alpha2) {
        // LATER: Add lazy instantiation and check if paint already created
        float x1 = (float) ((state.dx + x) * state.scale);
        float y1 = (float) ((state.dy + y) * state.scale);
        float x2 = x1;
        float y2 = y1;
        h *= state.scale;
        w *= state.scale;
        if (direction == null || direction == Direction.SOUTH) {
            y2 = (float) (y1 + h);
        } else if (direction == Direction.EAST) {
            x2 = (float) (x1 + w);
        } else if (direction == Direction.NORTH) {
            y1 = (float) (y1 + h);
        } else if (direction == Direction.WEST) {
            x1 = (float) (x1 + w);
        }
        Color c1 = color1;
        if (alpha1 != 1) {
            c1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), (int) (alpha1 * 255));
        }
        Color c2 = color2;
        if (alpha2 != 1) {
            c2 = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), (int) (alpha2 * 255));
        }
        state.gradientPaint = new GradientPaint(x1, y1, c1, x2, y2, c2, true);
    }

    @Override
    public void setShadow(boolean value) {
        state.shadow = value;
    }

    @Override
    public void setShadowColor(Color value) {
        state.shadowColor = value;
    }

    @Override
    public void setShadowAlpha(double value) {
        state.shadowAlpha = value;
    }

    @Override
    public void setShadowOffset(double dx, double dy) {
        state.shadowOffsetX = dx;
        state.shadowOffsetY = dy;
    }

    @Override
    public void rect(double x, double y, double w, double h) {
        currentPath = new GeneralPath();
        currentPath.append(
                new Rectangle2D.Double((state.dx + x) * state.scale, (state.dy + y) * state.scale, w * state.scale,
                                       h * state.scale), false);
    }

    /**
     * Implements a rounded rectangle using a path.
     */
    @Override
    public void roundrect(double x, double y, double w, double h, double dx, double dy) {
        // LATER: Use arc here or quad in VML/SVG for exact match
        begin();
        moveTo(x + dx, y);
        lineTo(x + w - dx, y);
        quadTo(x + w, y, x + w, y + dy);
        lineTo(x + w, y + h - dy);
        quadTo(x + w, y + h, x + w - dx, y + h);
        lineTo(x + dx, y + h);
        quadTo(x, y + h, x, y + h - dy);
        lineTo(x, y + dy);
        quadTo(x, y, x + dx, y);
    }

    @Override
    public void ellipse(double x, double y, double w, double h) {
        currentPath = new GeneralPath();
        currentPath.append(
                new Ellipse2D.Double((state.dx + x) * state.scale, (state.dy + y) * state.scale, w * state.scale,
                                     h * state.scale), false);
    }

    @Override
    public void image(double x, double y, double w, double h, String src, boolean aspect, boolean flipH,
                      boolean flipV) {
        if (src != null && w > 0 && h > 0) {
            Image img = loadImage(src);
            if (img != null) {
                Rectangle bounds = getImageBounds(img, x, y, w, h, aspect);
                img = scaleImage(img, bounds.width, bounds.height);
                if (img != null) {
                    drawImage(createImageGraphics(bounds.x, bounds.y, bounds.width, bounds.height, flipH, flipV), img,
                              bounds.x, bounds.y);
                }
            }
        }
    }

    /**
     * Hook for image caching.
     */
    protected Image loadImage(String src) {
        return Utils.loadImage(src);
    }

    protected final Rectangle getImageBounds(Image img, double x, double y, double w, double h, boolean aspect) {
        x = (state.dx + x) * state.scale;
        y = (state.dy + y) * state.scale;
        w *= state.scale;
        h *= state.scale;
        if (aspect) {
            Dimension size = getImageSize(img);
            double s = Math.min(w / size.width, h / size.height);
            int sw = (int) Math.round(size.width * s);
            int sh = (int) Math.round(size.height * s);
            x += (w - sw) / 2;
            y += (h - sh) / 2;
            w = sw;
            h = sh;
        } else {
            w = Math.round(w);
            h = Math.round(h);
        }
        return new Rectangle((int) x, (int) y, (int) w, (int) h);
    }

    /**
     * Returns the size for the given image.
     */
    protected Dimension getImageSize(Image image) {
        return new Dimension(image.getWidth(null), image.getHeight(null));
    }

    /**
     * Uses {@link #IMAGE_SCALING} to scale the given image.
     */
    protected Image scaleImage(Image img, int w, int h) {
        Dimension size = getImageSize(img);
        if (w == size.width && h == size.height) {
            return img;
        } else {
            return img.getScaledInstance(w, h, IMAGE_SCALING);
        }
    }

    protected void drawImage(Graphics2D graphics, Image image, int x, int y) {
        graphics.drawImage(image, x, y, null);
    }

    /**
     * Creates a graphic instance for rendering an image.
     */
    protected final Graphics2D createImageGraphics(double x, double y, double w, double h, boolean flipH,
                                                   boolean flipV) {
        Graphics2D g2 = state.g;
        if (flipH || flipV) {
            g2 = (Graphics2D) g2.create();
            if (flipV && flipH) {
                g2.rotate(Math.toRadians(180), x + w / 2, y + h / 2);
            } else {
                int sx = 1;
                int sy = 1;
                int dx = 0;
                int dy = 0;
                if (flipH) {
                    sx = -1;
                    dx = (int) (-w - 2 * x);
                }
                if (flipV) {
                    sy = -1;
                    dy = (int) (-h - 2 * y);
                }
                g2.scale(sx, sy);
                g2.translate(dx, dy);
            }
        }
        return g2;
    }

    /**
     * Draws the given text.
     */
    @Override
    public void text(double x, double y, double w, double h, String str, String align, String valign, boolean wrap,
                     String format, String overflow, boolean clip, double rotation, String textDirection) {
        plainText(x, y, w, h, str, align, valign, wrap, format, overflow, clip, rotation);
    }

    /**
     * Draws the given text.
     */
    public void plainText(double x, double y, double w, double h, String str, String align, String valign, boolean wrap,
                          String format, String overflow, boolean clip, double rotation) {
        if (state.fontColor != null) {
            x = (state.dx + x) * state.scale;
            y = (state.dy + y) * state.scale;
            w *= state.scale;
            h *= state.scale;
            // Font-metrics needed below this line
            Graphics2D g2 = createTextGraphics(x, y, w, h, rotation, clip, align, valign);
            FontMetrics fm = g2.getFontMetrics();
            String[] lines = str.split("\n");
            int[] stringWidths = new int[lines.length];
            int textWidth = 0;
            for (int i = 0; i < lines.length; i++) {
                stringWidths[i] = fm.stringWidth(lines[i]);
                textWidth = Math.max(textWidth, stringWidths[i]);
            }
            int textHeight = Math.round(lines.length * (fm.getFont().getSize() * Constants.LINE_HEIGHT));
            if (clip && textHeight > h && h > 0) {
                textHeight = (int) h;
            }
            Point2D margin = getMargin(align, valign);
            x += margin.getX() * textWidth;
            y += margin.getY() * textHeight;
            if (state.fontBackgroundColor != null) {
                g2.setColor(state.fontBackgroundColor);
                g2.fillRect((int) Math.round(x), (int) Math.round(y - 1), textWidth + 1, textHeight + 2);
            }
            if (state.fontBorderColor != null) {
                g2.setColor(state.fontBorderColor);
                g2.drawRect((int) Math.round(x), (int) Math.round(y - 1), textWidth + 1, textHeight + 2);
            }
            g2.setColor(state.fontColor);
            y += fm.getHeight() - fm.getDescent() - (margin.getY() + 0.5);
            for (int i = 0; i < lines.length; i++) {
                double dx = 0;
                if (align != null) {
                    if (align.equals(Constants.ALIGN_CENTER)) {
                        dx = (textWidth - stringWidths[i]) / 2;
                    } else if (align.equals(Constants.ALIGN_RIGHT)) {
                        dx = textWidth - stringWidths[i];
                    }
                }
                // Adds support for underlined text via attributed character iterator
                if (!lines[i].isEmpty()) {
                    boolean isUnderline = (state.fontStyle & Constants.FONT_UNDERLINE) == Constants.FONT_UNDERLINE;
                    boolean isStrikethrough = (state.fontStyle & Constants.FONT_STRIKETHROUGH)
                                              == Constants.FONT_STRIKETHROUGH;
                    if (isUnderline || isStrikethrough) {
                        AttributedString as = new AttributedString(lines[i]);
                        as.addAttribute(TextAttribute.FONT, g2.getFont());
                        if (isUnderline) {
                            as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                        }
                        if (isStrikethrough) {
                            as.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                        }
                        g2.drawString(as.getIterator(), (int) Math.round(x + dx), (int) Math.round(y));
                    } else {
                        g2.drawString(lines[i], (int) Math.round(x + dx), (int) Math.round(y));
                    }
                }
                y += Math.round(fm.getFont().getSize() * Constants.LINE_HEIGHT);
            }
        }
    }

    /**
     * Returns a new graphics instance with the correct color and font for
     * text rendering.
     */
    protected final Graphics2D createTextGraphics(double x, double y, double w, double h, double rotation, boolean clip,
                                                  String align, String valign) {
        Graphics2D g2 = state.g;
        updateFont();
        if (rotation != 0) {
            g2 = (Graphics2D) state.g.create();
            double rad = rotation * (Math.PI / 180);
            g2.rotate(rad, x, y);
        }
        if (clip && w > 0 && h > 0) {
            if (g2 == state.g) {
                g2 = (Graphics2D) state.g.create();
            }
            Point2D margin = getMargin(align, valign);
            x += margin.getX() * w;
            y += margin.getY() * h;
            g2.clip(new Rectangle2D.Double(x, y, w, h));
        }
        return g2;
    }

    protected Point2D getMargin(String align, String valign) {
        double dx = 0;
        double dy = 0;
        if (align != null) {
            if (align.equals(Constants.ALIGN_CENTER)) {
                dx = -0.5;
            } else if (align.equals(Constants.ALIGN_RIGHT)) {
                dx = -1;
            }
        }
        if (valign != null) {
            if (valign.equals(Constants.ALIGN_MIDDLE)) {
                dy = -0.5;
            } else if (valign.equals(Constants.ALIGN_BOTTOM)) {
                dy = -1;
            }
        }
        return new Point2D.Double(dx, dy);
    }

    protected void updateFont() {
        int size = (int) Math.round(state.fontSize * state.scale);
        int style = ((state.fontStyle & Constants.FONT_BOLD) == Constants.FONT_BOLD) ? Font.BOLD : Font.PLAIN;
        style += ((state.fontStyle & Constants.FONT_ITALIC) == Constants.FONT_ITALIC) ? Font.ITALIC : Font.PLAIN;
        if (lastFont == null
            || !lastFontFamily.equals(state.fontFamily)
            || size != lastFontSize
            || style != lastFontStyle) {
            lastFont = createFont(state.fontFamily, style, size);
            lastFontFamily = state.fontFamily;
            lastFontStyle = style;
            lastFontSize = size;
        }
        state.g.setFont(lastFont);
    }

    /**
     * Hook for subclassers to implement font caching.
     */
    protected Font createFont(String family, int style, int size) {
        return new Font(getFontName(family), style, size);
    }

    /**
     * Returns a font name for the given CSS values for font-family.
     * This implementation returns the first entry for comma-separated
     * lists of entries.
     */
    protected String getFontName(String family) {
        if (family != null) {
            int comma = family.indexOf(',');
            if (comma >= 0) {
                family = family.substring(0, comma);
            }
        }
        return family;
    }

    @Override
    public void begin() {
        currentPath = new GeneralPath();
    }

    @Override
    public void moveTo(double x, double y) {
        if (currentPath != null) {
            currentPath.moveTo((float) ((state.dx + x) * state.scale), (float) ((state.dy + y) * state.scale));
        }
    }

    @Override
    public void lineTo(double x, double y) {
        if (currentPath != null) {
            currentPath.lineTo((float) ((state.dx + x) * state.scale), (float) ((state.dy + y) * state.scale));
        }
    }

    @Override
    public void quadTo(double x1, double y1, double x2, double y2) {
        if (currentPath != null) {
            currentPath.quadTo((float) ((state.dx + x1) * state.scale), (float) ((state.dy + y1) * state.scale),
                               (float) ((state.dx + x2) * state.scale), (float) ((state.dy + y2) * state.scale));
        }
    }

    @Override
    public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        if (currentPath != null) {
            currentPath.curveTo((float) ((state.dx + x1) * state.scale), (float) ((state.dy + y1) * state.scale),
                                (float) ((state.dx + x2) * state.scale), (float) ((state.dy + y2) * state.scale),
                                (float) ((state.dx + x3) * state.scale), (float) ((state.dy + y3) * state.scale));
        }
    }

    /**
     * Closes the current path.
     */
    @Override
    public void close() {
        if (currentPath != null) {
            currentPath.closePath();
        }
    }

    @Override
    public void stroke() {
        paintCurrentPath(false, true);
    }

    @Override
    public void fill() {
        paintCurrentPath(true, false);
    }

    @Override
    public void fillAndStroke() {
        paintCurrentPath(true, true);
    }

    protected void paintCurrentPath(boolean filled, boolean stroked) {
        if (currentPath != null) {
            if (stroked) {
                if (state.strokeColor != null) {
                    updateStroke();
                }
            }
            if (state.shadow) {
                paintShadow(filled, stroked);
            }
            if (filled) {
                if (state.gradientPaint != null) {
                    state.g.setPaint(state.gradientPaint);
                    state.g.fill(currentPath);
                } else {
                    if (state.fillColor != null) {
                        state.g.setColor(state.fillColor);
                        state.g.setPaint(null);
                        state.g.fill(currentPath);
                    }
                }
            }
            if (stroked && state.strokeColor != null) {
                state.g.setColor(state.strokeColor);
                state.g.draw(currentPath);
            }
        }
    }

    /**
     * Returns a clone of the given state.
     */
    protected CanvasState cloneState(CanvasState state) {
        try {
            return (CanvasState) state.clone();
        } catch (CloneNotSupportedException e) {
            log.log(Level.SEVERE, "Failed to clone the state", e);
        }
        return null;
    }

    protected void updateStroke() {
        float sw = (float) Math.max(1, state.strokeWidth * state.scale);
        int cap = BasicStroke.CAP_BUTT;
        if (state.lineCap.equals("round")) {
            cap = BasicStroke.CAP_ROUND;
        } else if (state.lineCap.equals("square")) {
            cap = BasicStroke.CAP_SQUARE;
        }
        int join = BasicStroke.JOIN_MITER;
        if (state.lineJoin.equals("round")) {
            join = BasicStroke.JOIN_ROUND;
        } else if (state.lineJoin.equals("bevel")) {
            join = BasicStroke.JOIN_BEVEL;
        }
        float miterlimit = (float) state.miterLimit;
        if (lastStroke == null
            || lastStrokeWidth != sw
            || lastCap != cap
            || lastJoin != join
            || lastMiterLimit != miterlimit
            || lastDashed != state.dashed
            || (state.dashed && lastDashPattern != state.dashPattern)) {
            float[] dash = null;
            if (state.dashed) {
                dash = new float[state.dashPattern.length];
                for (int i = 0; i < dash.length; i++) {
                    dash[i] = (float) (state.dashPattern[i] * ((state.fixDash) ? state.scale : sw));
                }
            }
            lastStroke = new BasicStroke(sw, cap, join, miterlimit, dash, 0);
            lastStrokeWidth = sw;
            lastCap = cap;
            lastJoin = join;
            lastMiterLimit = miterlimit;
            lastDashed = state.dashed;
            lastDashPattern = state.dashPattern;
        }
        state.g.setStroke(lastStroke);
    }

    protected void paintShadow(boolean filled, boolean stroked) {
        if (state.shadowColor != null) {
            double rad = -state.theta * (Math.PI / 180);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double dx = state.shadowOffsetX * state.scale;
            double dy = state.shadowOffsetY * state.scale;
            if (state.flipH) {
                dx *= -1;
            }
            if (state.flipV) {
                dy *= -1;
            }
            double tx = dx * cos - dy * sin;
            double ty = dx * sin + dy * cos;
            state.g.setColor(state.shadowColor);
            state.g.translate(tx, ty);
            double alpha = state.alpha * state.shadowAlpha;
            Composite comp = state.g.getComposite();
            state.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (alpha)));
            if (filled && (state.gradientPaint != null || state.fillColor != null)) {
                state.g.fill(currentPath);
            }
            // FIXME: Overlaps with fill in composide mode
            if (stroked && state.strokeColor != null) {
                state.g.draw(currentPath);
            }
            state.g.translate(-tx, -ty);
            state.g.setComposite(comp);
        }
    }

    protected static class CanvasState implements Cloneable {

        protected double alpha = 1;
        protected double scale = 1;
        protected double dx = 0;
        protected double dy = 0;
        protected double theta = 0;
        protected double rotationCx = 0;
        protected double rotationCy = 0;
        protected boolean flipV = false;
        protected boolean flipH = false;
        protected double miterLimit = 10;
        protected int fontStyle = 0;
        protected double fontSize = 11;
        protected String fontFamily = "Arial,Helvetica";
        protected Color fontColor = Color.BLACK;
        protected Color fontBackgroundColor;
        protected Color fontBorderColor;
        protected String lineCap = "flat";
        protected String lineJoin = "miter";
        protected double strokeWidth = 1;
        protected Color strokeColor;
        protected Color fillColor;
        protected Paint gradientPaint;
        protected boolean dashed = false;
        protected boolean fixDash = false;
        protected float[] dashPattern = {3, 3};
        protected boolean shadow = false;
        protected Color shadowColor = Color.GRAY;
        protected double shadowAlpha = 1;
        protected double shadowOffsetX = 2;
        protected double shadowOffsetY = 3;
        /**
         * Stores the actual state.
         */
        protected transient Graphics2D g;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
