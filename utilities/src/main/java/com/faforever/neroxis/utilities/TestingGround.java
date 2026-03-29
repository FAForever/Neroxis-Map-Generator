package com.faforever.neroxis.utilities;

import com.faforever.neroxis.util.vector.Vector2;

public class TestingGround {
    void main() {
        Vector2 vector1 = new Vector2(1, 1);
        Vector2 vector2 = new Vector2(0, 1);

        System.out.println(vector1.angleTo(vector2));
        System.out.println(vector1.getAngle(vector2));
    }
}
