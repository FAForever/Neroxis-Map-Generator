package biomes;

import java.util.List;

// used in disk operations to be converted into a material later
public strictfp class Biome {
    public String name;
    public List<BiomeMaterial> materials;
    public BiomeMaterialElement macroTexture;
}

strictfp class BiomeMaterial{
    public BiomeMaterialElement texture;
    public BiomeMaterialElement normal;
}

strictfp class BiomeMaterialElement{
    public String environment;
    public String name;
    public float scale;
}