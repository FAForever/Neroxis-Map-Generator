package com.faforever.neroxis.util.vector;

import java.util.LinkedHashSet;

public strictfp class Vector3 extends Vector<Vector3> {
    public Vector3() {
        super(3);
    }

    public Vector3(Vector3 other) {
        this(other.getX(), other.getY(), other.getZ());
    }

    public Vector3(Vector2 other) {
        this(other.getX(), 0f, other.getY());
    }

    public Vector3(float x, float y, float z) {
        super(x, y, z);
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

    public float getZ() {
        return components[Vector.Z];
    }

    public void setZ(float z) {
        components[Vector.Z] = z;
    }

    public Vector3 cross(Vector3 other) {
        Vector3 cross = new Vector3();
        float x = getX();
        float oX = other.getX();
        float y = getY();
        float oY = other.getY();
        float z = getZ();
        float oZ = other.getZ();
        cross.setX(y * oZ - z * oY);
        cross.setY(z * oX - x * oZ);
        cross.setZ(x * oY - y * oX);
        return cross;
    }

    public float getXZDistance(Vector2 location) {
        return getXZDistance(new Vector3(location));
    }

    public float getXZDistance(Vector3 location) {
        float dx = getX() - location.getX();
        float dz = getZ() - location.getZ();
        return (float) StrictMath.sqrt(dx * dx + dz * dz);
    }

    public LinkedHashSet<Vector2> getXZLine(Vector3 location) {
        LinkedHashSet<Vector2> line = new LinkedHashSet<>();
        Vector2 currentPoint = new Vector2(this);
        Vector2 targetPoint = new Vector2(location);
        while (currentPoint.getDistance(targetPoint) > 1) {
            line.add(currentPoint);
            float angle = currentPoint.angleTo(location);
            currentPoint = new Vector2(StrictMath.round(currentPoint.getX() + StrictMath.cos(angle)),
                                       StrictMath.round(currentPoint.getY() + StrictMath.sin(angle)));
        }
        return line;
    }

    public float getAzimuth() {
        return (float) StrictMath.atan2(getZ(), getX());
    }

    public float getElevation() {
        return (float) StrictMath.atan2(getY(), StrictMath.sqrt(getX() * getX() + getZ() * getZ()));
    }

    public Vector3 roundXYToNearestHalfPoint() {
        setX(StrictMath.round(getX() - .5f) + .5f);
        setZ(StrictMath.round(getZ() - .5f) + .5f);
        return this;
    }

    @Override
    public Vector3 copy() {
        return new Vector3(this);
    }
}
