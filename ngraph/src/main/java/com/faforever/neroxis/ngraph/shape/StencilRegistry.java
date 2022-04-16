package com.faforever.neroxis.ngraph.shape;

import java.util.HashMap;
import java.util.Map;

public class StencilRegistry {

    protected static Map<String, Stencil> stencils = new HashMap<>();

    /**
     * Adds the given stencil.
     */
    public static void addStencil(String name, Stencil stencil) {
        stencils.put(name, stencil);
    }

    /**
     * Returns the stencil for the given name.
     */
    public static Stencil getStencil(String name) {
        return stencils.get(name);
    }

}
