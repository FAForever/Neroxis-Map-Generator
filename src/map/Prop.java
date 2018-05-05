package map;

import util.Vector3f;

public strictfp class Prop {
	private final String path;
	private final Vector3f position;
	private final float rotation;

	public Prop(String path, Vector3f position, float rotation) {
		this.path = path;
		this.position = position;
		this.rotation = rotation;
	}

	public String getPath() {
		return path;
	}

	public Vector3f getPosition() {
		return position;
	}

	public float getRotation() {
		return rotation;
	}
}
