/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.util.Constants;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Singleton class that acts as a global converter from string to object values
 * in a style. This is currently only used to perimeters and edge styles.
 */
public class StyleRegistry {

    /**
     * Maps from strings to objects.
     */
    protected static Map<String, Object> values = new Hashtable<String, Object>();

    // Registers the known object styles
    static {
        putValue(Constants.EDGESTYLE_ELBOW, EdgeStyle.ElbowConnector);
        putValue(Constants.EDGESTYLE_ENTITY_RELATION, EdgeStyle.EntityRelation);
        putValue(Constants.EDGESTYLE_LOOP, EdgeStyle.Loop);
        putValue(Constants.EDGESTYLE_SIDETOSIDE, EdgeStyle.SideToSide);
        putValue(Constants.EDGESTYLE_TOPTOBOTTOM, EdgeStyle.TopToBottom);
        putValue(Constants.EDGESTYLE_ORTHOGONAL, EdgeStyle.OrthConnector);
        putValue(Constants.EDGESTYLE_SEGMENT, EdgeStyle.SegmentConnector);

        putValue(Constants.PERIMETER_ELLIPSE, Perimeter.EllipsePerimeter);
        putValue(Constants.PERIMETER_RECTANGLE, Perimeter.RectanglePerimeter);
        putValue(Constants.PERIMETER_RHOMBUS, Perimeter.RhombusPerimeter);
        putValue(Constants.PERIMETER_TRIANGLE, Perimeter.TrianglePerimeter);
        putValue(Constants.PERIMETER_HEXAGON, Perimeter.HexagonPerimeter);
    }

    /**
     * Puts the given object into the registry under the given name.
     */
    public static void putValue(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Returns the value associated with the given name.
     */
    public static Object getValue(String name) {
        return values.get(name);
    }

    /**
     * Returns the name for the given value.
     */
    public static String getName(Object value) {
        Iterator<Map.Entry<String, Object>> it = values.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();

            if (entry.getValue() == value) {
                return entry.getKey();
            }
        }

        return null;
    }

}
