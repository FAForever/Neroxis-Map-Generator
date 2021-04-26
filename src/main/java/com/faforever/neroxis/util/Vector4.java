package com.faforever.neroxis.util;

public strictfp class Vector4 extends Vector<Vector4> {

    public Vector4() {
        super(4);
    }

    public Vector4(float x, float y, float z, float w) {
        super(x, y, z, w);
    }

    public Vector4(Vector4 other) {
        this(other.getX(), other.getY(), other.getW(), other.getZ());
    }

    public float getX() {
        return components[Vector.X];
    }

    public void setX(float x) {
        components[0] = x;
    }

    public float getY() {
        return components[Vector.Y];
    }

    public void setY(float y) {
        components[1] = y;
    }

    public float getZ() {
        return components[Vector.Z];
    }

    public void setZ(float z) {
        components[2] = z;
    }

    public float getW() {
        return components[Vector.W];
    }

    public void setW(float w) {
        components[3] = w;
    }

    @Override
    public Vector4 copy() {
        return new Vector4(this);
    }
}
