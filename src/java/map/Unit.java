package map;

import util.Vector3f;

public strictfp class Unit {
    private String type;
    private Vector3f position;
    private float rotation;

    public Unit(String type, Vector3f position, float rotation) {
        this.type = type;
        this.position = position;
        this.rotation = rotation;
    }

    public String getType() {
        return type;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getRotation() {
        return rotation;
    }
}