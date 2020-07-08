package map;

public strictfp class TerrainMaterials {

    // engine limitations - must stay 9 and 10 always
    public static final int TERRAIN_TEXTURE_COUNT = 10;
    public static final int TERRAIN_NORMAL_COUNT = 9;

    public String[] texturePaths = new String[TERRAIN_TEXTURE_COUNT];
    public float[] textureScales = new float[TERRAIN_TEXTURE_COUNT];
    public String[] normalPaths = new String[TERRAIN_NORMAL_COUNT];
    public float[] normalScales = new float[TERRAIN_NORMAL_COUNT];


    public TerrainMaterials(Material[] materials, Material macroTexture) {
        for (int i = 0; i < TERRAIN_TEXTURE_COUNT; i++) {
            boolean isEmpty = i >= materials.length;
            if (i < TERRAIN_NORMAL_COUNT) {
                texturePaths[i] = isEmpty ? "" : materials[i].getTexturePath();
                textureScales[i] = isEmpty ? 4f : materials[i].getTextureScale();
                normalPaths[i] = isEmpty ? "" : materials[i].getNormalPath();
                normalScales[i] = isEmpty ? 4f : materials[i].getNormalScale();
            } else {
                texturePaths[i] = macroTexture.getTexturePath();
                textureScales[i] = macroTexture.getTextureScale();
            }
        }
    }
}
