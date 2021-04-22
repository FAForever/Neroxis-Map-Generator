package com.faforever.neroxis.util;

import lombok.Data;

@Data
public strictfp class Vector4f {
    private float x;
    private float y;
    private float z;
    private float w;

    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f, %f)", x, y, z, w);
    }
}
