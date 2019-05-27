package map;

public strictfp class TerrainMaterials {

    // engine limitations - must stay 9 and 10 always
    public final int TERRAIN_TEXTURE_COUNT = 10;
    public final int TERRAIN_NORMAL_COUNT = 9;

    public final String[] texturePaths = new String[TERRAIN_TEXTURE_COUNT];
    public final float[] textureScales = new float[TERRAIN_TEXTURE_COUNT];
    public final String[] normalPaths = new String[TERRAIN_NORMAL_COUNT];
    public final float[] normalScales = new float[TERRAIN_NORMAL_COUNT];


    public TerrainMaterials(Material[] materials, Material macroTexture){
        for(int i = 0; i < TERRAIN_TEXTURE_COUNT; i++) {
            boolean isEmpty = i >= materials.length;
            if (i < TERRAIN_NORMAL_COUNT) {
                texturePaths[i] = isEmpty ? "" : materials[i].texturePath;
                textureScales[i] = isEmpty ? 4f : materials[i].textureScale;
                normalPaths[i] = isEmpty ? "" : materials[i].normalPath;
                normalScales[i] = isEmpty ? 4f : materials[i].normalScale;
            }
            else{
                texturePaths[i] = macroTexture.texturePath;
                textureScales[i] = macroTexture.textureScale;
            }
        }
    }
}
