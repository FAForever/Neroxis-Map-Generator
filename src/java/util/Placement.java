package util;

import map.SCMap;

public strictfp class Placement {

    public static Vector3f placeOnHeightmap(SCMap map, Vector2f v) {
        return placeOnHeightmap(map, v.x, v.y);
    }

    public static Vector3f placeOnHeightmap(SCMap map, Vector3f v) {
        return placeOnHeightmap(map, v.x, v.z);
    }

    public static Vector3f placeOnHeightmap(SCMap map, float x, float z) {
        Vector3f v = new Vector3f(x, 0, z);
        v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
        return v;
    }
}
