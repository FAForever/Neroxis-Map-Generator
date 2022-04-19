/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.shape.ConnectorShape;
import com.faforever.neroxis.ngraph.shape.DefaultTextShape;
import com.faforever.neroxis.ngraph.shape.RectangleShape;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.style.arrow.ClassicArrow;
import com.faforever.neroxis.ngraph.style.perimeter.RectanglePerimeter;
import java.awt.Color;
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
     * Maps from names to styles.
     */
    protected Map<String, Style> styles = new HashMap<>();

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
    public Map<String, Style> getStyles() {
        return styles;
    }

    /**
     * Sets all styles in the stylesheet.
     */
    public void setStyles(Map<String, Style> styles) {
        this.styles = styles;
    }

    /**
     * Creates and returns the default vertex style.
     *
     * @return Returns the default vertex style.
     */
    protected Style createDefaultVertexStyle() {
        Style style = new Style(null);
        style.getShape().setShape(new RectangleShape());
        style.getPerimeter().setPerimeter(new RectanglePerimeter());
        style.getShape().setFillColor(new Color(195, 217, 255));
        style.getShape().setStrokeColor(new Color(100, 130, 185));
        style.getLabel().setTextColor(new Color(119, 68, 0));
        style.getLabel().setTextShape(new DefaultTextShape());
        return style;
    }

    /**
     * Creates and returns the default edge style.
     *
     * @return Returns the default edge style.
     */
    protected Style createDefaultEdgeStyle() {
        Style style = new Style(null);
        style.getShape().setShape(new ConnectorShape());
        style.getEdge().setEndArrow(new ClassicArrow());
        style.getShape().setStrokeColor(new Color(100, 130, 185));
        style.getLabel().setTextColor(new Color(68, 98, 153));
        style.getLabel().setTextShape(new DefaultTextShape());
        return style;
    }

    /**
     * Returns the default style for vertices.
     *
     * @return Returns the default vertex style.
     */
    public Style getDefaultVertexStyle() {
        return styles.get("defaultVertex");
    }

    /**
     * Sets the default style for vertices.
     *
     * @param value Style to be used for vertices.
     */
    public void setDefaultVertexStyle(Style value) {
        putCellStyle("defaultVertex", value);
    }

    /**
     * Returns the default style for edges.
     *
     * @return Returns the default edge style.
     */
    public Style getDefaultEdgeStyle() {
        return styles.get("defaultEdge");
    }

    /**
     * Sets the default style for edges.
     *
     * @param value Style to be used for edges.
     */
    public void setDefaultEdgeStyle(Style value) {
        putCellStyle("defaultEdge", value);
    }

    /**
     * Stores the specified style under the given name.
     *
     * @param name  Name for the style to be stored.
     * @param style Key, value pairs that define the style.
     */
    public void putCellStyle(String name, Style style) {
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
    public Style getCellStyle(String name, Style defaultStyle) {
        return styles.getOrDefault(name, defaultStyle).spawnChild();
    }

}
