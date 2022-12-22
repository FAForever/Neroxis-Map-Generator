package com.faforever.neroxis.util.vector;

import com.faforever.neroxis.map.Symmetry;

import java.awt.*;
import java.util.LinkedHashSet;

public class Vector2 extends Vector<Vector2> {
    public Vector2() {
        super(2);
    }

    public Vector2(Vector2 other) {
        this(other.getX(), other.getY());
    }

    public Vector2(float x, float y) {
        super(x, y);
    }

    public Vector2(Vector3 location) {
        super(2);
        setX(location.getX());
        setY(location.getZ());
    }

    public Vector2(Dimension other) {
        this((float) other.getWidth(), (float) other.getHeight());
    }

    public Vector2(Point other) {
        this((float) other.getX(), (float) other.getY());
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

    public void set(Dimension other) {
        setX((float) other.getWidth());
        setY((float) other.getHeight());
    }

    public void set(Point other) {
        setX((float) other.getX());
        setY((float) other.getY());
    }

    public float angleTo(Vector3 location) {
        return angleTo(new Vector2(location));
    }

    public float angleTo(Vector2 location) {
        float dx = location.getX() - getX();
        float dy = location.getY() - getY();
        return (float) StrictMath.atan2(dy, dx);
    }

    public LinkedHashSet<Vector2> getLine(Vector2 location) {
        LinkedHashSet<Vector2> line = new LinkedHashSet<>();
        Vector2 currentPoint = this;
        while (currentPoint.getDistance(location) > 1) {
            line.add(currentPoint);
            float angle = currentPoint.angleTo(location);
            currentPoint = new Vector2((float) (currentPoint.getX() + StrictMath.cos(angle)),
                                       (float) (currentPoint.getY() + StrictMath.sin(angle)));
        }
        return line;
    }

    public Vector2 addPolar(float angle, float magnitude) {
        return add((float) (magnitude * StrictMath.cos(angle)), (float) (magnitude * StrictMath.sin(angle)));
    }

    public void flip(Vector2 center, Symmetry symmetry) {
        switch (symmetry) {
            case X -> setX(2 * center.getX() - getX());
            case Z -> setY(2 * center.getY() - getY());
            case XZ, ZX, POINT2 -> {
                setX(2 * center.getX() - getX());
                setY(2 * center.getY() - getY());
            }
        }
    }

    public Vector2 rotate(float angle) {
        float oldX = getX();
        float oldY = getY();
        float cos = (float) StrictMath.cos(angle);
        float sin = (float) StrictMath.sin(angle);
        setX(oldX * cos - oldY * sin);
        setY(oldX * sin + oldY * cos);
        return this;
    }

    @Override
    public Vector2 copy() {
        return new Vector2(this);
    }
}
