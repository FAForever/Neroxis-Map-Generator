package com.faforever.neroxis.util.vector;

public class Vector4 extends Vector<Vector4> {
    public Vector4() {
        super(4);
    }

    public Vector4(Vector4 other) {
        this(other.getX(), other.getY(), other.getZ(), other.getW());
    }

    public Vector4(float x, float y, float z, float w) {
        super(x, y, z, w);
    }

    public float getX() {
        return components[Vector.X];
    }

    public void setX(float x) {
        components[Vector.X] = x;
    }

    public float getY() {
        return components[Vector.Y];
    }

    public void setY(float y) {
        components[Vector.Y] = y;
    }

    public float getW() {
        return components[Vector.W];
    }

    public void setW(float w) {
        components[Vector.W] = w;
    }

    public float getZ() {
        return components[Vector.Z];
    }

    public void setZ(float z) {
        components[Vector.Z] = z;
    }

    @Override
    public Vector4 copy() {
        return new Vector4(this);
    }
}
