package com.faforever.neroxis.util.vector;

import com.faforever.neroxis.util.functional.FloatConsumer;
import com.faforever.neroxis.util.functional.FloatSupplier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class Vector3 extends Vector<Vector3> {
    private float x;
    private float y;
    private float z;

    public Vector3(Vector2 other) {
        this(other.getX(), 0f, other.getY());
    }

    public Vector3(Vector3 other) {
        this(other.getX(), other.getY(), other.getZ());
    }

    @Override
    protected VectorComponentGetter<Vector3> getComponentGetter(int i) {
        return switch (i) {
            case Vector.X -> Vector3::getX;
            case Vector.Y -> Vector3::getY;
            case Vector.Z -> Vector3::getZ;
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    protected VectorComponentSetter<Vector3> getComponentSetter(int i) {
        return switch (i) {
            case Vector.X -> Vector3::setX;
            case Vector.Y -> Vector3::setY;
            case Vector.Z -> Vector3::setZ;
            default -> throw new UnsupportedOperationException("Unsupported component: " + i);
        };
    }

    @Override
    public int getDimension() {
        return 3;
    }

    @Override
    public float[] toArray() {
        return new float[]{x, y, z};
    }

    public Vector3 cross(Vector3 other) {
        float x = getX();
        float oX = other.getX();
        float y = getY();
        float oY = other.getY();
        float z = getZ();
        float oZ = other.getZ();
        float newX = y * oZ - z * oY;
        float newY = z * oX - x * oZ;
        float newZ = x * oY - y * oX;
        return new Vector3(newX, newY, newZ);
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
