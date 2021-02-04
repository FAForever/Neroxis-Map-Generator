package neroxis.util;

import lombok.Data;
import neroxis.map.Symmetry;

import java.util.LinkedHashSet;

@Data
public strictfp class Vector2f {
    private float x;
    private float y;

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f(Vector2f location) {
        this(location.x, location.y);
    }

    public Vector2f(Vector3f location) {
        this.x = location.getX();
        this.y = location.getZ();
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

    public Vector2f addPolar(float angle, float magnitude) {
        this.x += magnitude * StrictMath.cos(angle);
        this.y += magnitude * StrictMath.sin(angle);
        return this;
    }

    public Vector2f subtract(Vector2f vector) {
        return subtract(vector.x, vector.y);
    }

    public Vector2f subtract(float x, float y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vector2f multiply(float multiplier) {
        this.x *= multiplier;
        this.y *= multiplier;
        return this;
    }

    public Vector2f clampMin(float x, float y) {
        this.x = StrictMath.max(x, this.x);
        this.y = StrictMath.max(y, this.y);
        return this;
    }

    public Vector2f clampMax(float x, float y) {
        this.x = StrictMath.min(x, this.x);
        this.y = StrictMath.min(y, this.y);
        return this;
    }

    public Vector2f clear() {
        this.x = 0;
        this.y = 0;
        return this;
    }

    public void flip(Vector2f center, Symmetry symmetry) {
        switch (symmetry) {
            case X:
                x = 2 * center.x - x;
                break;
            case Z:
                y = 2 * center.y - y;
                break;
            case XZ:
            case ZX:
            case POINT2:
                x = 2 * center.x - x;
                y = 2 * center.y - y;
                break;
        }
    }

    public void roundToNearestHalfPoint() {
        x = StrictMath.round(x - .5f) + .5f;
        y = StrictMath.round(y - .5f) + .5f;
    }

    public void round() {
        x = StrictMath.round(x);
        y = StrictMath.round(y);
    }

    @Override
    public String toString() {
        return String.format("(%f, %f)", x, y);
    }
}
