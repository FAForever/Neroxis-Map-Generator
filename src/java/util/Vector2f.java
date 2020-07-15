package util;

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
}
