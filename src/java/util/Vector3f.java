package util;

import lombok.EqualsAndHashCode;

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

    public float getAzimuth() {
        return (float) StrictMath.toDegrees(StrictMath.atan2(z, x));
    }

    public float getElevation() {
        return (float) StrictMath.toDegrees(StrictMath.atan2(y, StrictMath.sqrt(x * x + z * z)));
    }

    public Vector3f add(Vector3f vector) {
        return new Vector3f(x + vector.x, y + vector.y, z + vector.z);
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f)", x, y, z);
    }
}
