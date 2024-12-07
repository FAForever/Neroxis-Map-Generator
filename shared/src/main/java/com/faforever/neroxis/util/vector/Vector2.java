package com.faforever.neroxis.util.vector;

import com.faforever.neroxis.map.Symmetry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.awt.Dimension;
import java.awt.Point;
import java.util.LinkedHashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class Vector2 extends Vector<Vector2> {
    private float x;
    private float y;

    public Vector2(Vector2 other) {
        this(other.getX(), other.getY());
    }

    public Vector2(Vector3 location) {
        setX(location.getX());
        setY(location.getZ());
    }

    public Vector2(Dimension other) {
        this((float) other.getWidth(), (float) other.getHeight());
    }

    public Vector2(Point other) {
        this((float) other.getX(), (float) other.getY());
    }

    @Override
    protected VectorComponentGetter<Vector2> getComponentGetter(int i) {
        return switch (i) {
            case Vector.X -> Vector2::getX;
            case Vector.Y -> Vector2::getY;
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    protected VectorComponentSetter<Vector2> getComponentSetter(int i) {
        return switch (i) {
            case Vector.X -> Vector2::setX;
            case Vector.Y -> Vector2::setY;
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public float[] toArray() {
        return new float[]{x, y};
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
