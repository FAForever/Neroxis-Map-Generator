package util;

import lombok.EqualsAndHashCode;
import map.Symmetry;

import java.util.LinkedHashSet;

@EqualsAndHashCode
public strictfp class Vector2f {
    public float x;
    public float y;

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f(Vector3f location) {
        this.x = location.x;
        this.y = location.z;
    }

    public float getDistance(Vector3f location) {
        return getDistance(new Vector2f(location));
    }

    public float getDistance(Vector2f location) {
        float dx = x - location.x;
        float dy = y - location.y;
        return (float) StrictMath.sqrt(dx * dx + dy * dy);
    }

    public float getAngle(Vector3f location) {
        return getAngle(new Vector2f(location));
    }

    public float getAngle(Vector2f location) {
        float dx = location.x - x;
        float dy = location.y - y;
        return (float) StrictMath.atan2(dy, dx);
    }

    public LinkedHashSet<Vector2f> getLine(Vector2f location) {
        LinkedHashSet<Vector2f> line = new LinkedHashSet<>();
        Vector2f currentPoint = this;
        while (currentPoint.getDistance(location) > .1) {
            line.add(currentPoint);
            float angle = currentPoint.getAngle(location);
            currentPoint = new Vector2f(StrictMath.round(currentPoint.x + StrictMath.cos(angle)), StrictMath.round(currentPoint.y + StrictMath.sin(angle)));
        }
        return line;
    }

    public Vector2f add(Vector2f vector) {
        return add(vector.x, vector.y);
    }

    public Vector2f add(float x, float y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public void flip(Vector2f center, Symmetry symmetry) {
        switch (symmetry) {
            case X -> x = 2 * center.x - x;
            case Z -> y = 2 * center.y - y;
            case XZ, ZX, POINT -> {
                x = 2 * center.x - x;
                y = 2 * center.y - y;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("(%f, %f)", x, y);
    }
}
