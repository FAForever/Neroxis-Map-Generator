package neroxis.util;

import neroxis.map.SCMap;

import java.awt.image.BufferedImage;

public strictfp class Placement {

    public static Vector3f placeOnHeightmap(SCMap map, Vector2f v) {
        return placeOnHeightmap(map, v.x, v.y);
    }

    public static Vector3f placeOnHeightmap(SCMap map, Vector3f v) {
        return placeOnHeightmap(map, v.x, v.z);
    }

    public static Vector3f placeOnHeightmap(SCMap map, float x, float z) {
        Vector3f v = new Vector3f(x, 0, z);
        BufferedImage heightMap = map.getHeightmap();
        if ((int) x < heightMap.getWidth() && (int) x > 0 && (int) z < heightMap.getHeight() && (int) z > 0) {
            v.y = heightMap.getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (map.getHeightMapScale());
        }
        return v;
    }
}
