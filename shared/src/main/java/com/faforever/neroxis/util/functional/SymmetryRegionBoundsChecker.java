package com.faforever.neroxis.util.functional;

import com.faforever.neroxis.util.vector.Vector2;

public interface SymmetryRegionBoundsChecker {

    boolean inBounds(int x, int y);

    default boolean inBounds(Vector2 location) {
        return inBounds((int) location.getX(), (int) location.getY());
    }

}
