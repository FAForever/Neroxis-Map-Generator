/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.style.perimeter.RectanglePerimeter;
import com.faforever.neroxis.ngraph.util.Constants;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines the appearance of the cells in a graph. The following example
 * changes the font size for all vertices by changing the default vertex
 * style in-place:
 * <code>
 * getDefaultVertexStyle().put(Constants.STYLE_FONTSIZE, 16);
 * </code>
 * <p>
 * To change the default font size for all cells, set
 * Constants.DEFAULT_FONTSIZE.
 */
public class Stylesheet {
    /**
     * Shared immutable empty hashtable (for undefined cell styles).
     */
    public static final Map<String, Object> EMPTY_STYLE = new HashMap<>();
    /**
     * Maps from names to styles.
     */
    protected Map<String, Map<String, Object>> styles = new HashMap<>();

    /**
     * Constructs a new stylesheet and assigns default styles.
     */
    public Stylesheet() {
        setDefaultVertexStyle(createDefaultVertexStyle());
        setDefaultEdgeStyle(createDefaultEdgeStyle());
    }

    /**
     * Returns all styles as map of name, hashtable pairs.
     *
     * @return All styles in this stylesheet.
     */
    public Map<String, Map<String, Object>> getStyles() {
        return styles;
    }

    /**
     * Sets all styles in the stylesheet.
     */
    public void setStyles(Map<String, Map<String, Object>> styles) {
        this.styles = styles;
    }

    /**
     * Creates and returns the default vertex style.
     *
     * @return Returns the default vertex style.
     */
    protected Map<String, Object> createDefaultVertexStyle() {
        Map<String, Object> style = new HashMap<>();
        style.put(Constants.STYLE_SHAPE, Constants.SHAPE_RECTANGLE);
        style.put(Constants.STYLE_PERIMETER, new RectanglePerimeter());
        style.put(Constants.STYLE_VERTICAL_ALIGN, Constants.ALIGN_MIDDLE);
        style.put(Constants.STYLE_ALIGN, Constants.ALIGN_CENTER);
        style.put(Constants.STYLE_FILLCOLOR, "#C3D9FF");
        style.put(Constants.STYLE_STROKECOLOR, "#6482B9");
        style.put(Constants.STYLE_FONTCOLOR, "#774400");

        return style;
    }

    /**
     * Creates and returns the default edge style.
     *
     * @return Returns the default edge style.
     */
    protected Map<String, Object> createDefaultEdgeStyle() {
        Map<String, Object> style = new HashMap<>();

        style.put(Constants.STYLE_SHAPE, Constants.SHAPE_CONNECTOR);
        style.put(Constants.STYLE_ENDARROW, Constants.ARROW_CLASSIC);
        style.put(Constants.STYLE_VERTICAL_ALIGN, Constants.ALIGN_MIDDLE);
        style.put(Constants.STYLE_ALIGN, Constants.ALIGN_CENTER);
        style.put(Constants.STYLE_STROKECOLOR, "#6482B9");
        style.put(Constants.STYLE_FONTCOLOR, "#446299");

        return style;
    }

    /**
     * Returns the default style for vertices.
     *
     * @return Returns the default vertex style.
     */
    public Map<String, Object> getDefaultVertexStyle() {
        return styles.get("defaultVertex");
    }

    /**
     * Sets the default style for vertices.
     *
     * @param value Style to be used for vertices.
     */
    public void setDefaultVertexStyle(Map<String, Object> value) {
        putCellStyle("defaultVertex", value);
    }

    /**
     * Returns the default style for edges.
     *
     * @return Returns the default edge style.
     */
    public Map<String, Object> getDefaultEdgeStyle() {
        return styles.get("defaultEdge");
    }

    /**
     * Sets the default style for edges.
     *
     * @param value Style to be used for edges.
     */
    public void setDefaultEdgeStyle(Map<String, Object> value) {
        putCellStyle("defaultEdge", value);
    }

    /**
     * Stores the specified style under the given name.
     *
     * @param name  Name for the style to be stored.
     * @param style Key, value pairs that define the style.
     */
    public void putCellStyle(String name, Map<String, Object> style) {
        styles.put(name, style);
    }

    /**
     * Returns the cell style for the specified cell or the given defaultStyle
     * if no style can be found for the given stylename.
     *
     * @param name         String of the form [(stylename|key=value);] that represents the
     *                     style.
     * @param defaultStyle Default style to be returned if no style can be found.
     * @return Returns the style for the given formatted cell style.
     */
    public Map<String, Object> getCellStyle(String name, Map<String, Object> defaultStyle) {
        Map<String, Object> style = defaultStyle;

        if (name != null && name.length() > 0) {
            String[] pairs = name.split(";");

            if (style != null && !name.startsWith(";")) {
                style = new HashMap<>(style);
            } else {
                style = new HashMap<>();
            }
            for (String tmp : pairs) {
                int c = tmp.indexOf('=');
                if (c >= 0) {
                    String key = tmp.substring(0, c);
                    String value = tmp.substring(c + 1);
                    if (value.equals(Constants.NONE)) {
                        style.remove(key);
                    } else {
                        style.put(key, value);
                    }
                } else {
                    Map<String, Object> tmpStyle = styles.get(tmp);
                    if (tmpStyle != null) {
                        style.putAll(tmpStyle);
                    }
                }
            }
        }

        return style;
    }

}
