package util;

public strictfp class Vector3f {
    public float x;
    public float y;
    public float z;

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
}
