/**
 * Copyright (c) 2007-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.shape.ActorShape;
import com.faforever.neroxis.ngraph.shape.ArrowShape;
import com.faforever.neroxis.ngraph.shape.CloudShape;
import com.faforever.neroxis.ngraph.shape.ConnectorShape;
import com.faforever.neroxis.ngraph.shape.CurveShape;
import com.faforever.neroxis.ngraph.shape.CylinderShape;
import com.faforever.neroxis.ngraph.shape.DefaultTextShape;
import com.faforever.neroxis.ngraph.shape.DoubleEllipseShape;
import com.faforever.neroxis.ngraph.shape.DoubleRectangleShape;
import com.faforever.neroxis.ngraph.shape.EllipseShape;
import com.faforever.neroxis.ngraph.shape.HexagonShape;
import com.faforever.neroxis.ngraph.shape.IShape;
import com.faforever.neroxis.ngraph.shape.ITextShape;
import com.faforever.neroxis.ngraph.shape.ImageShape;
import com.faforever.neroxis.ngraph.shape.LabelShape;
import com.faforever.neroxis.ngraph.shape.LineShape;
import com.faforever.neroxis.ngraph.shape.RectangleShape;
import com.faforever.neroxis.ngraph.shape.RhombusShape;
import com.faforever.neroxis.ngraph.shape.SwimlaneShape;
import com.faforever.neroxis.ngraph.shape.TriangleShape;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.style.util.Direction;
import com.faforever.neroxis.ngraph.swing.util.SwingConstants;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.view.CellState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of a canvas that uses Graphics2D for painting.
 */
public class Graphics2DCanvas extends BasicCanvas {
    public static final double LINE_ARCSIZE = 10;
    public static final String TEXT_SHAPE_DEFAULT = "default";
    public static final String TEXT_SHAPE_HTML = "html";
    private static final Logger log = Logger.getLogger(Graphics2DCanvas.class.getName());
    /**
     * Specifies the image scaling quality. Default is Image.SCALE_SMOOTH.
     */
    public static int IMAGE_SCALING = Image.SCALE_SMOOTH;
    /**
     * Maps from names to IVertexShape instances.
     */
    protected static Map<String, IShape> shapes = new HashMap<>();
    /**
     * Maps from names to ITextShape instances. There are currently three different
     * hardcoded text shapes available here: default, html and wrapped.
     */
    protected static Map<String, ITextShape> textShapes = new HashMap<>();

    /*
     * Static initializer.
     */
    static {
        putShape(Constants.SHAPE_ACTOR, new ActorShape());
        putShape(Constants.SHAPE_ARROW, new ArrowShape());
        putShape(Constants.SHAPE_CLOUD, new CloudShape());
        putShape(Constants.SHAPE_CONNECTOR, new ConnectorShape());
        putShape(Constants.SHAPE_CYLINDER, new CylinderShape());
        putShape(Constants.SHAPE_CURVE, new CurveShape());
        putShape(Constants.SHAPE_DOUBLE_RECTANGLE, new DoubleRectangleShape());
        putShape(Constants.SHAPE_DOUBLE_ELLIPSE, new DoubleEllipseShape());
        putShape(Constants.SHAPE_ELLIPSE, new EllipseShape());
        putShape(Constants.SHAPE_HEXAGON, new HexagonShape());
        putShape(Constants.SHAPE_IMAGE, new ImageShape());
        putShape(Constants.SHAPE_LABEL, new LabelShape());
        putShape(Constants.SHAPE_LINE, new LineShape());
        putShape(Constants.SHAPE_RECTANGLE, new RectangleShape());
        putShape(Constants.SHAPE_RHOMBUS, new RhombusShape());
        putShape(Constants.SHAPE_SWIMLANE, new SwimlaneShape());
        putShape(Constants.SHAPE_TRIANGLE, new TriangleShape());
        putTextShape(TEXT_SHAPE_DEFAULT, new DefaultTextShape());
    }

    /**
     * Optional renderer pane to be used for HTML label rendering.
     */
    protected CellRendererPane rendererPane;
    /**
     * Global graphics handle to the image.
     */
    protected Graphics2D graphics2D;

    /**
     * Constructs a new graphics canvas with an empty graphics object.
     */
    public Graphics2DCanvas() {
        this(null);
    }

    /**
     * Constructs a new graphics canvas for the given graphics object.
     */
    public Graphics2DCanvas(Graphics2D graphics2D) {
        this.graphics2D = graphics2D;

        // Initializes the cell renderer pane for drawing HTML markup
        try {
            rendererPane = new CellRendererPane();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to initialize renderer pane", e);
        }
    }

    public static void putShape(String name, IShape shape) {
        shapes.put(name, shape);
    }

    public static void putTextShape(String name, ITextShape shape) {
        textShapes.put(name, shape);
    }

    public ITextShape getTextShape(Style style) {
        return textShapes.get(TEXT_SHAPE_DEFAULT);
    }

    public CellRendererPane getRendererPane() {
        return rendererPane;
    }

    /**
     * Returns the graphics object for this canvas.
     */
    public Graphics2D getGraphics() {
        return graphics2D;
    }

    /**
     * Sets the graphics object for this canvas.
     */
    public void setGraphics(Graphics2D graphics2D) {
        this.graphics2D = graphics2D;
    }

    @Override
    public Object drawCell(CellState state) {
        Style style = state.getStyle();
        IShape shape = getShape(style);

        if (graphics2D != null && shape != null) {
            // Creates a temporary graphics instance for drawing this shape
            float opacity = style.getShape().getOpacity();
            Graphics2D previousGraphics = graphics2D;
            graphics2D = createTemporaryGraphics(style, opacity, state);

            // Paints the shape and restores the graphics object
            shape.paintShape(this, state);
            graphics2D.dispose();
            graphics2D = previousGraphics;
        }

        return shape;
    }

    @Override
    public Object drawLabel(String text, CellState state) {
        Style style = state.getStyle();
        ITextShape shape = style.getLabel().getTextShape();
        if (graphics2D != null && shape != null && drawLabels && text != null && text.length() > 0) {
            // Creates a temporary graphics instance for drawing this shape
            float opacity = style.getLabel().getTextOpacity();
            Graphics2D previousGraphics = graphics2D;
            graphics2D = createTemporaryGraphics(style, opacity, null);
            // Draws the label background and border
            Color bg = style.getLabel().getBackgroundColor();
            Color border = style.getLabel().getBorderColor();
            paintRectangle(state.getLabelBounds().getRectangle(), bg, border);
            // Paints the label and restores the graphics object
            shape.paintShape(this, text, state, style);
            graphics2D.dispose();
            graphics2D = previousGraphics;
        }

        return shape;
    }

    public IShape getShape(Style style) {
        return style.getShape().getShape();
    }

    public Graphics2D createTemporaryGraphics(Style style, float opacity, RectangleDouble bounds) {
        Graphics2D temporaryGraphics = (Graphics2D) graphics2D.create();
        // Applies the default translate
        temporaryGraphics.translate(translate.getX(), translate.getY());
        // Applies the rotation on the graphics object
        if (bounds != null) {
            double rotation = style.getShape().getRotation();
            if (rotation != 0) {
                temporaryGraphics.rotate(Math.toRadians(rotation), bounds.getCenterX(), bounds.getCenterY());
            }
        }

        // Applies the opacity to the graphics object
        if (opacity != 100) {
            temporaryGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity / 100));
        }

        return temporaryGraphics;
    }

    public void drawImage(java.awt.Rectangle bounds, String imageUrl) {
        drawImage(bounds, imageUrl, PRESERVE_IMAGE_ASPECT, false, false);
    }

    public void drawImage(java.awt.Rectangle bounds, String imageUrl, boolean preserveAspect, boolean flipH,
                          boolean flipV) {
        if (imageUrl != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            Image img = loadImage(imageUrl);

            if (img != null) {
                int w, h;
                int x = bounds.x;
                int y = bounds.y;
                Dimension size = getImageSize(img);

                if (preserveAspect) {
                    double s = Math.min(bounds.width / (double) size.width, bounds.height / (double) size.height);
                    w = (int) (size.width * s);
                    h = (int) (size.height * s);
                    x += (bounds.width - w) / 2;
                    y += (bounds.height - h) / 2;
                } else {
                    w = bounds.width;
                    h = bounds.height;
                }

                Image scaledImage = (w == size.width && h == size.height) ? img : img.getScaledInstance(w, h,
                                                                                                        IMAGE_SCALING);

                if (scaledImage != null) {
                    AffineTransform af = null;

                    if (flipH || flipV) {
                        af = graphics2D.getTransform();
                        int sx = 1;
                        int sy = 1;
                        int dx = 0;
                        int dy = 0;

                        if (flipH) {
                            sx = -1;
                            dx = -w - 2 * x;
                        }

                        if (flipV) {
                            sy = -1;
                            dy = -h - 2 * y;
                        }

                        graphics2D.scale(sx, sy);
                        graphics2D.translate(dx, dy);
                    }

                    drawImageImpl(scaledImage, x, y);

                    // Restores the previous transform
                    if (af != null) {
                        graphics2D.setTransform(af);
                    }
                }
            }
        }
    }

    /**
     * Returns the size for the given image.
     */
    protected Dimension getImageSize(Image image) {
        return new Dimension(image.getWidth(null), image.getHeight(null));
    }

    /**
     * Implements the actual graphics call.
     */
    protected void drawImageImpl(Image image, int x, int y) {
        graphics2D.drawImage(image, x, y, null);
    }

    public void paintPolyline(PointDouble[] points, boolean rounded) {
        if (points != null && points.length > 1) {
            PointDouble pt = points[0];
            PointDouble pe = points[points.length - 1];
            double arcSize = Constants.LINE_ARCSIZE * scale;
            GeneralPath path = new GeneralPath();
            path.moveTo((float) pt.getX(), (float) pt.getY());
            // Draws the line segments
            for (int i = 1; i < points.length - 1; i++) {
                PointDouble tmp = points[i];
                double dx = pt.getX() - tmp.getX();
                double dy = pt.getY() - tmp.getY();

                if ((rounded && i < points.length - 1) && (dx != 0 || dy != 0)) {
                    // Draws a line from the last point to the current
                    // point with a spacing of size off the current point
                    // into direction of the last point
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double nx1 = dx * Math.min(arcSize, dist / 2) / dist;
                    double ny1 = dy * Math.min(arcSize, dist / 2) / dist;

                    double x1 = tmp.getX() + nx1;
                    double y1 = tmp.getY() + ny1;
                    path.lineTo((float) x1, (float) y1);

                    // Draws a curve from the last point to the current
                    // point with a spacing of size off the current point
                    // into direction of the next point
                    PointDouble next = points[i + 1];

                    // Uses next non-overlapping point
                    while (i < points.length - 2
                           && Math.round(next.getX() - tmp.getX()) == 0
                           && Math.round(next.getY() - tmp.getY()) == 0) {
                        next = points[i + 2];
                        i++;
                    }

                    dx = next.getX() - tmp.getX();
                    dy = next.getY() - tmp.getY();

                    dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                    double nx2 = dx * Math.min(arcSize, dist / 2) / dist;
                    double ny2 = dy * Math.min(arcSize, dist / 2) / dist;

                    double x2 = tmp.getX() + nx2;
                    double y2 = tmp.getY() + ny2;
                    path.quadTo((float) tmp.getX(), (float) tmp.getY(), (float) x2, (float) y2);
                    tmp = new PointDouble(x2, y2);
                } else {
                    path.lineTo((float) tmp.getX(), (float) tmp.getY());
                }

                pt = tmp;
            }

            path.lineTo((float) pe.getX(), (float) pe.getY());
            graphics2D.draw(path);
        }
    }

    public void paintRectangle(java.awt.Rectangle bounds, Color background, Color border) {
        if (background != null) {
            graphics2D.setColor(background);
            fillShape(bounds);
        }

        if (border != null) {
            graphics2D.setColor(border);
            graphics2D.draw(bounds);
        }
    }

    public void fillShape(Shape shape) {
        fillShape(shape, false);
    }

    public void fillShape(Shape shape, boolean shadow) {
        int shadowOffsetX = (shadow) ? Constants.SHADOW_OFFSETX : 0;
        int shadowOffsetY = (shadow) ? Constants.SHADOW_OFFSETY : 0;

        if (shadow) {
            // Saves the state and configures the graphics object
            Paint p = graphics2D.getPaint();
            Color previousColor = graphics2D.getColor();
            graphics2D.setColor(SwingConstants.SHADOW_COLOR);
            graphics2D.translate(shadowOffsetX, shadowOffsetY);
            // Paints the shadow
            fillShape(shape, false);
            // Restores the state of the graphics object
            graphics2D.translate(-shadowOffsetX, -shadowOffsetY);
            graphics2D.setColor(previousColor);
            graphics2D.setPaint(p);
        }
        graphics2D.fill(shape);
    }

    public Stroke createStroke(Style style) {
        double width = style.getShape().getStrokeWidth() * scale;
        boolean dashed = style.getEdge().isDashed();
        if (dashed) {
            float[] dashPattern = style.getEdge().getDashPattern();
            float[] scaledDashPattern = new float[dashPattern.length];
            for (int i = 0; i < dashPattern.length; i++) {
                scaledDashPattern[i] = (float) (dashPattern[i] * scale * width);
            }
            return new BasicStroke((float) width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
                                   scaledDashPattern, 0.0f);
        } else {
            return new BasicStroke((float) width);
        }
    }

    public Paint createFillPaint(RectangleDouble bounds, Style style) {
        Color fillColor = style.getShape().getFillColor();
        Paint fillPaint = null;
        if (fillColor != null) {
            Color gradientColor = style.getShape().getGradientColor();
            if (gradientColor != null) {
                Direction gradientDirection = style.getShape().getGradientDirection();
                float x1 = (float) bounds.getX();
                float y1 = (float) bounds.getY();
                float x2 = (float) bounds.getX();
                float y2 = (float) bounds.getY();
                if (gradientDirection == null || gradientDirection == Direction.SOUTH) {
                    y2 = (float) (bounds.getY() + bounds.getHeight());
                } else if (gradientDirection == Direction.EAST) {
                    x2 = (float) (bounds.getX() + bounds.getWidth());
                } else if (gradientDirection == Direction.NORTH) {
                    y1 = (float) (bounds.getY() + bounds.getHeight());
                } else if (gradientDirection == Direction.WEST) {
                    x1 = (float) (bounds.getX() + bounds.getWidth());
                }

                fillPaint = new GradientPaint(x1, y1, fillColor, x2, y2, gradientColor, true);
            }
        }

        return fillPaint;
    }
}
