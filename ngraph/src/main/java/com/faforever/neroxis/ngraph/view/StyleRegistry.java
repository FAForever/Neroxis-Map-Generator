/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.style.edge.ElbowConnectorEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.EntityRelationEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.LoopEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.OrthConnectorEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.SegmentConnectorEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.SideToSideEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.edge.TopToBottomEdgeStyleFunction;
import com.faforever.neroxis.ngraph.style.perimeter.EllipsePerimeter;
import com.faforever.neroxis.ngraph.style.perimeter.HexagonPerimeter;
import com.faforever.neroxis.ngraph.style.perimeter.RectanglePerimeter;
import com.faforever.neroxis.ngraph.style.perimeter.RhombusPerimeter;
import com.faforever.neroxis.ngraph.style.perimeter.TrianglePerimeter;
import com.faforever.neroxis.ngraph.util.Constants;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class that acts as a global converter from string to object values
 * in a style. This is currently only used to perimeters and edge styles.
 */
public class StyleRegistry {
    /**
     * Maps from strings to objects.
     */
    protected static Map<String, Object> values = new HashMap<>();

    // Registers the known object styles
    static {
        putValue(Constants.EDGESTYLE_ELBOW, new ElbowConnectorEdgeStyleFunction());
        putValue(Constants.EDGESTYLE_ENTITY_RELATION, new EntityRelationEdgeStyleFunction());
        putValue(Constants.EDGESTYLE_LOOP, new LoopEdgeStyleFunction());
        putValue(Constants.EDGESTYLE_SIDETOSIDE, new SideToSideEdgeStyleFunction());
        putValue(Constants.EDGESTYLE_TOPTOBOTTOM, new TopToBottomEdgeStyleFunction());
        putValue(Constants.EDGESTYLE_ORTHOGONAL, new OrthConnectorEdgeStyleFunction());
        putValue(Constants.EDGESTYLE_SEGMENT, new SegmentConnectorEdgeStyleFunction());
        putValue(Constants.PERIMETER_ELLIPSE, new EllipsePerimeter());
        putValue(Constants.PERIMETER_RECTANGLE, new RectanglePerimeter());
        putValue(Constants.PERIMETER_RHOMBUS, new RhombusPerimeter());
        putValue(Constants.PERIMETER_TRIANGLE, new TrianglePerimeter());
        putValue(Constants.PERIMETER_HEXAGON, new HexagonPerimeter());
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
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() == value) {
                return entry.getKey();
            }
        }

        return null;
    }

}
