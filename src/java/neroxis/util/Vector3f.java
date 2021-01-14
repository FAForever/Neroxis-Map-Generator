package neroxis.util;

import lombok.EqualsAndHashCode;

import java.util.LinkedHashSet;

@EqualsAndHashCode
public strictfp class Vector3f {
    public float x;
    public float y;
    public float z;

    public Vector3f(Vector2f location) {
        this.x = location.x;
        this.y = 0;
        this.z = location.y;
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getXZDistance(Vector2f location) {
        return getXZDistance(new Vector3f(location));
    }

    public float getXZDistance(Vector3f location) {
        float dx = x - location.x;
        float dz = z - location.z;
        return (float) StrictMath.sqrt(dx * dx + dz * dz);
    }

    public LinkedHashSet<Vector2f> getXZLine(Vector3f location) {
        LinkedHashSet<Vector2f> line = new LinkedHashSet<>();
        Vector2f currentPoint = new Vector2f(this);
        Vector2f targetPoint = new Vector2f(location);
        while (currentPoint.getDistance(targetPoint) > 1) {
            line.add(currentPoint);
            float angle = currentPoint.getAngle(location);
            currentPoint = new Vector2f(StrictMath.round(currentPoint.x + StrictMath.cos(angle)), StrictMath.round(currentPoint.y + StrictMath.sin(angle)));
        }
        return line;
    }

    public float getAzimuth() {
        return (float) StrictMath.toDegrees(StrictMath.atan2(z, x));
    }

    public float getElevation() {
        return (float) StrictMath.toDegrees(StrictMath.atan2(y, StrictMath.sqrt(x * x + z * z)));
    }

    public Vector3f add(Vector3f vector) {
        return new Vector3f(x + vector.x, y + vector.y, z + vector.z);
    }

    public void roundXYToNearestHalfPoint() {
        x = StrictMath.round(x * 2) / 2f;
        z = StrictMath.round(y * 2) / 2f;
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f)", x, y, z);
    }
}
