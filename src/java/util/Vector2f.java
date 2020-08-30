package util;

import lombok.EqualsAndHashCode;

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

    @Override
    public String toString(){
        return String.format("(%f, %f)", x, y);
    }
}
