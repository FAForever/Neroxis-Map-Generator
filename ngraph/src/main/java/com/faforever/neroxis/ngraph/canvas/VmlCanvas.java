/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Point;
import com.faforever.neroxis.ngraph.util.Rectangle;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a canvas that uses VML for painting.
 */
public class VmlCanvas extends BasicCanvas {

    /**
     * Holds the HTML document that represents the canvas.
     */
    protected Document document;

    /**
     * Constructs a new VML canvas for the specified dimension and scale.
     */
    public VmlCanvas() {
        this(null);
    }

    /**
     * Constructs a new VML canvas for the specified bounds, scale and
     * background color.
     */
    public VmlCanvas(Document document) {
        setDocument(document);
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
    public void appendVmlElement(Element node) {
        if (document != null) {
            Node body = document.getDocumentElement().getFirstChild().getNextSibling();

            if (body != null) {
                body.appendChild(node);
            }
        }

    }

    /* (non-Javadoc)
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
            Element strokeNode = document.createElement("v:stroke");

            // Draws the markers
            String start = Utils.getString(style, Constants.STYLE_STARTARROW);
            String end = Utils.getString(style, Constants.STYLE_ENDARROW);

            if (start != null || end != null) {
                if (start != null) {
                    strokeNode.setAttribute("startarrow", start);

                    String startWidth = "medium";
                    String startLength = "medium";
                    double startSize = Utils.getFloat(style, Constants.STYLE_STARTSIZE, Constants.DEFAULT_MARKERSIZE) * scale;

                    if (startSize < 6) {
                        startWidth = "narrow";
                        startLength = "short";
                    } else if (startSize > 10) {
                        startWidth = "wide";
                        startLength = "long";
                    }

                    strokeNode.setAttribute("startarrowwidth", startWidth);
                    strokeNode.setAttribute("startarrowlength", startLength);
                }

                if (end != null) {
                    strokeNode.setAttribute("endarrow", end);

                    String endWidth = "medium";
                    String endLength = "medium";
                    double endSize = Utils.getFloat(style, Constants.STYLE_ENDSIZE, Constants.DEFAULT_MARKERSIZE) * scale;

                    if (endSize < 6) {
                        endWidth = "narrow";
                        endLength = "short";
                    } else if (endSize > 10) {
                        endWidth = "wide";
                        endLength = "long";
                    }

                    strokeNode.setAttribute("endarrowwidth", endWidth);
                    strokeNode.setAttribute("endarrowlength", endLength);
                }
            }

            if (Utils.isTrue(style, Constants.STYLE_DASHED)) {
                strokeNode.setAttribute("dashstyle", "2 2");
            }

            elem.appendChild(strokeNode);
        } else {
            int x = (int) (state.getX() + translate.getX());
            int y = (int) (state.getY() + translate.getY());
            int w = (int) state.getWidth();
            int h = (int) state.getHeight();

            if (!Utils.getString(style, Constants.STYLE_SHAPE, "").equals(Constants.SHAPE_SWIMLANE)) {
                elem = drawShape(x, y, w, h, style);

                if (Utils.isTrue(style, Constants.STYLE_DASHED)) {
                    Element strokeNode = document.createElement("v:stroke");
                    strokeNode.setAttribute("dashstyle", "2 2");
                    elem.appendChild(strokeNode);
                }
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
        String fillColor = Utils.getString(style, Constants.STYLE_FILLCOLOR);
        String strokeColor = Utils.getString(style, Constants.STYLE_STROKECOLOR);
        float strokeWidth = (float) (Utils.getFloat(style, Constants.STYLE_STROKEWIDTH, 1) * scale);

        // Draws the shape
        String shape = Utils.getString(style, Constants.STYLE_SHAPE);
        Element elem = null;

        if (shape.equals(Constants.SHAPE_IMAGE)) {
            String img = getImageForStyle(style);

            if (img != null) {
                elem = document.createElement("v:img");
                elem.setAttribute("src", img);
            }
        } else if (shape.equals(Constants.SHAPE_LINE)) {
            String direction = Utils.getString(style, Constants.STYLE_DIRECTION, Constants.DIRECTION_EAST);
            String points = null;

            if (direction.equals(Constants.DIRECTION_EAST) || direction.equals(Constants.DIRECTION_WEST)) {
                int mid = Math.round(h / 2);
                points = "m 0 " + mid + " l " + w + " " + mid;
            } else {
                int mid = Math.round(w / 2);
                points = "m " + mid + " 0 L " + mid + " " + h;
            }

            elem = document.createElement("v:shape");
            elem.setAttribute("coordsize", w + " " + h);
            elem.setAttribute("path", points + " x e");
        } else if (shape.equals(Constants.SHAPE_ELLIPSE)) {
            elem = document.createElement("v:oval");
        } else if (shape.equals(Constants.SHAPE_DOUBLE_ELLIPSE)) {
            elem = document.createElement("v:shape");
            elem.setAttribute("coordsize", w + " " + h);
            int inset = (int) ((3 + strokeWidth) * scale);

            String points = "ar 0 0 " + w + " " + h + " 0 " + (h / 2) + " " + (w / 2) + " " + (h / 2) + " e ar " + inset + " " + inset + " " + (w - inset) + " " + (h - inset) + " 0 " + (h / 2) + " " + (w / 2) + " " + (h / 2);

            elem.setAttribute("path", points + " x e");
        } else if (shape.equals(Constants.SHAPE_RHOMBUS)) {
            elem = document.createElement("v:shape");
            elem.setAttribute("coordsize", w + " " + h);

            String points = "m " + (w / 2) + " 0 l " + w + " " + (h / 2) + " l " + (w / 2) + " " + h + " l 0 " + (h / 2);

            elem.setAttribute("path", points + " x e");
        } else if (shape.equals(Constants.SHAPE_TRIANGLE)) {
            elem = document.createElement("v:shape");
            elem.setAttribute("coordsize", w + " " + h);

            String direction = Utils.getString(style, Constants.STYLE_DIRECTION, "");
            String points = null;

            if (direction.equals(Constants.DIRECTION_NORTH)) {
                points = "m 0 " + h + " l " + (w / 2) + " 0 " + " l " + w + " " + h;
            } else if (direction.equals(Constants.DIRECTION_SOUTH)) {
                points = "m 0 0 l " + (w / 2) + " " + h + " l " + w + " 0";
            } else if (direction.equals(Constants.DIRECTION_WEST)) {
                points = "m " + w + " 0 l " + w + " " + (h / 2) + " l " + w + " " + h;
            } else
            // east
            {
                points = "m 0 0 l " + w + " " + (h / 2) + " l 0 " + h;
            }

            elem.setAttribute("path", points + " x e");
        } else if (shape.equals(Constants.SHAPE_HEXAGON)) {
            elem = document.createElement("v:shape");
            elem.setAttribute("coordsize", w + " " + h);

            String direction = Utils.getString(style, Constants.STYLE_DIRECTION, "");
            String points = null;

            if (direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_SOUTH)) {
                points = "m " + (int) (0.5 * w) + " 0 l " + w + " " + (int) (0.25 * h) + " l " + w + " " + (int) (0.75 * h) + " l " + (int) (0.5 * w) + " " + h + " l 0 " + (int) (0.75 * h) + " l 0 " + (int) (0.25 * h);
            } else {
                points = "m " + (int) (0.25 * w) + " 0 l " + (int) (0.75 * w) + " 0 l " + w + " " + (int) (0.5 * h) + " l " + (int) (0.75 * w) + " " + h + " l " + (int) (0.25 * w) + " " + h + " l 0 " + (int) (0.5 * h);
            }

            elem.setAttribute("path", points + " x e");
        } else if (shape.equals(Constants.SHAPE_CLOUD)) {
            elem = document.createElement("v:shape");
            elem.setAttribute("coordsize", w + " " + h);

            String points = "m " + (int) (0.25 * w) + " " + (int) (0.25 * h) + " c " + (int) (0.05 * w) + " " + (int) (0.25 * h) + " 0 " + (int) (0.5 * h) + " " + (int) (0.16 * w) + " " + (int) (0.55 * h) + " c 0 " + (int) (0.66 * h) + " " + (int) (0.18 * w) + " " + (int) (0.9 * h) + " " + (int) (0.31 * w) + " " + (int) (0.8 * h) + " c " + (int) (0.4 * w) + " " + (h) + " " + (int) (0.7 * w) + " " + (h) + " " + (int) (0.8 * w) + " " + (int) (0.8 * h) + " c " + (w) + " " + (int) (0.8 * h) + " " + (w) + " " + (int) (0.6 * h) + " " + (int) (0.875 * w) + " " + (int) (0.5 * h) + " c " + (w) + " " + (int) (0.3 * h) + " " + (int) (0.8 * w) + " " + (int) (0.1 * h) + " " + (int) (0.625 * w) + " " + (int) (0.2 * h) + " c " + (int) (0.5 * w) + " " + (int) (0.05 * h) + " " + (int) (0.3 * w) + " " + (int) (0.05 * h) + " " + (int) (0.25 * w) + " " + (int) (0.25 * h);

            elem.setAttribute("path", points + " x e");
        } else if (shape.equals(Constants.SHAPE_ACTOR)) {
            elem = document.createElement("v:shape");
            elem.setAttribute("coordsize", w + " " + h);

            double width3 = w / 3;
            String points = "m 0 " + (h) + " C 0 " + (3 * h / 5) + " 0 " + (2 * h / 5) + " " + (w / 2) + " " + (2 * h / 5) + " c " + (int) (w / 2 - width3) + " " + (2 * h / 5) + " " + (int) (w / 2 - width3) + " 0 " + (w / 2) + " 0 c " + (int) (w / 2 + width3) + " 0 " + (int) (w / 2 + width3) + " " + (2 * h / 5) + " " + (w / 2) + " " + (2 * h / 5) + " c " + (w) + " " + (2 * h / 5) + " " + (w) + " " + (3 * h / 5) + " " + (w) + " " + (h);

            elem.setAttribute("path", points + " x e");
        } else if (shape.equals(Constants.SHAPE_CYLINDER)) {
            elem = document.createElement("v:shape");
            elem.setAttribute("coordsize", w + " " + h);

            double dy = Math.min(40, Math.floor(h / 5));
            String points = "m 0 " + (int) (dy) + " C 0 " + (int) (dy / 3) + " " + (w) + " " + (int) (dy / 3) + " " + (w) + " " + (int) (dy) + " L " + (w) + " " + (int) (h - dy) + " C " + (w) + " " + (int) (h + dy / 3) + " 0 " + (int) (h + dy / 3) + " 0 " + (int) (h - dy) + " x e" + " m 0 " + (int) (dy) + " C 0 " + (int) (2 * dy) + " " + (w) + " " + (int) (2 * dy) + " " + (w) + " " + (int) (dy);

            elem.setAttribute("path", points + " e");
        } else {
            if (Utils.isTrue(style, Constants.STYLE_ROUNDED, false)) {
                elem = document.createElement("v:roundrect");
                elem.setAttribute("arcsize", (Constants.RECTANGLE_ROUNDING_FACTOR * 100) + "%");
            } else {
                elem = document.createElement("v:rect");
            }
        }

        String s = "position:absolute;left:" + x + "px;top:" + y + "px;width:" + w + "px;height:" + h + "px;";

        // Applies rotation
        double rotation = Utils.getDouble(style, Constants.STYLE_ROTATION);

        if (rotation != 0) {
            s += "rotation:" + rotation + ";";
        }

        elem.setAttribute("style", s);

        // Adds the shadow element
        if (Utils.isTrue(style, Constants.STYLE_SHADOW, false) && fillColor != null) {
            Element shadow = document.createElement("v:shadow");
            shadow.setAttribute("on", "true");
            shadow.setAttribute("color", Constants.W3C_SHADOWCOLOR);
            elem.appendChild(shadow);
        }

        float opacity = Utils.getFloat(style, Constants.STYLE_OPACITY, 100);
        float fillOpacity = Utils.getFloat(style, Constants.STYLE_FILL_OPACITY, 100);
        float strokeOpacity = Utils.getFloat(style, Constants.STYLE_STROKE_OPACITY, 100);

        // Applies opacity to fill
        if (fillColor != null) {
            Element fill = document.createElement("v:fill");
            fill.setAttribute("color", fillColor);

            if (opacity != 100 || fillOpacity != 100) {
                fill.setAttribute("opacity", String.valueOf(opacity * fillOpacity / 10000));
            }

            elem.appendChild(fill);
        } else {
            elem.setAttribute("filled", "false");
        }

        // Applies opacity to stroke
        if (strokeColor != null) {
            elem.setAttribute("strokecolor", strokeColor);
            Element stroke = document.createElement("v:stroke");

            if (opacity != 100 || strokeOpacity != 100) {
                stroke.setAttribute("opacity", String.valueOf(opacity * strokeOpacity / 10000));
            }

            elem.appendChild(stroke);
        } else {
            elem.setAttribute("stroked", "false");
        }

        elem.setAttribute("strokeweight", strokeWidth + "px");
        appendVmlElement(elem);

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
        String strokeColor = Utils.getString(style, Constants.STYLE_STROKECOLOR);
        float strokeWidth = (float) (Utils.getFloat(style, Constants.STYLE_STROKEWIDTH, 1) * scale);

        Element elem = document.createElement("v:shape");

        if (strokeColor != null && strokeWidth > 0) {
            Point pt = pts.get(0);
            java.awt.Rectangle r = new java.awt.Rectangle(pt.getPoint());

            StringBuilder buf = new StringBuilder("m " + Math.round(pt.getX()) + " " + Math.round(pt.getY()));

            for (int i = 1; i < pts.size(); i++) {
                pt = pts.get(i);
                buf.append(" l " + Math.round(pt.getX()) + " " + Math.round(pt.getY()));

                r = r.union(new java.awt.Rectangle(pt.getPoint()));
            }

            String d = buf.toString();
            elem.setAttribute("path", d);
            elem.setAttribute("filled", "false");
            elem.setAttribute("strokecolor", strokeColor);
            elem.setAttribute("strokeweight", strokeWidth + "px");

            String s = "position:absolute;" + "left:" + r.x + "px;" + "top:" + r.y + "px;" + "width:" + r.width + "px;" + "height:" + r.height + "px;";
            elem.setAttribute("style", s);

            elem.setAttribute("coordorigin", r.x + " " + r.y);
            elem.setAttribute("coordsize", r.width + " " + r.height);
        }

        appendVmlElement(elem);

        return elem;
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
    public Element drawText(String text, int x, int y, int w, int h, Map<String, Object> style) {
        Element table = Utils.createTable(document, text, x, y, w, h, scale, style);
        appendVmlElement(table);

        return table;
    }

}
