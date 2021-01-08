package neroxis.util;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public strictfp class Vector4f {
    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f, %f)", x, y, z, w);
    }
}
