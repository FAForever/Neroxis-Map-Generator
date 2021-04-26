package com.faforever.neroxis.util;

import com.faforever.neroxis.map.Symmetry;

import java.util.LinkedHashSet;

public strictfp class Vector2 extends Vector<Vector2> {

    public Vector2() {
        super(2);
    }

    public Vector2(float x, float y) {
        super(x, y);
    }

    public Vector2(Vector2 other) {
        this(other.getX(), other.getY());
    }

    public Vector2(Vector3 location) {
        super(2);
        setX(location.getX());
        setY(location.getZ());
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

    public float getAngle(Vector3 location) {
        return getAngle(new Vector2(location));
    }

    public float getAngle(Vector2 location) {
        float dx = location.getX() - getX();
        float dy = location.getY() - getY();
        return (float) StrictMath.atan2(dy, dx);
    }

    public LinkedHashSet<Vector2> getLine(Vector2 location) {
        LinkedHashSet<Vector2> line = new LinkedHashSet<>();
        Vector2 currentPoint = this;
        while (currentPoint.getDistance(location) > .1) {
            line.add(currentPoint);
            float angle = currentPoint.getAngle(location);
            currentPoint = new Vector2((float) (currentPoint.getX() + StrictMath.cos(angle)), (float) (currentPoint.getY() + StrictMath.sin(angle))).round();
        }
        return line;
    }

    public Vector2 addPolar(float angle, float magnitude) {
        return add((float) (magnitude * StrictMath.cos(angle)), (float) (magnitude * StrictMath.sin(angle)));
    }

    public void flip(Vector2 center, Symmetry symmetry) {
        switch (symmetry) {
            case X:
                setX(2 * center.getX() - getX());
                break;
            case Z:
                setY(2 * center.getY() - getY());
                break;
            case XZ:
            case ZX:
            case POINT2:
                setX(2 * center.getX() - getX());
                setY(2 * center.getY() - getY());
                break;
        }
    }

    public Vector2 roundToNearestHalfPoint() {
        setX(StrictMath.round(getX() - .5f) + .5f);
        setY(StrictMath.round(getY() - .5f) + .5f);
        return this;
    }

    @Override
    public Vector2 copy() {
        return new Vector2(this);
    }
}
