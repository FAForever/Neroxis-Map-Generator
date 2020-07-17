package util;

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

	public float getAzimuth() {
		return (float) StrictMath.toDegrees(StrictMath.atan2(z, x));
	}

	public float getElevation() {
		return (float) StrictMath.toDegrees(StrictMath.atan2(y, StrictMath.sqrt(x * x + z * z)));
	}

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vector3f other = (Vector3f) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        if (z != other.z)
            return false;
        return true;
    }
}
