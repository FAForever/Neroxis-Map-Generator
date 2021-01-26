package neroxis.map;

import lombok.Data;

@Data
public strictfp class DecalMaterials {
    private String[] fieldNormals;
    private String[] fieldAlbedos;
    private String[] mountainNormals;
    private String[] mountainAlbedos;
    private String[] slopeNormals;
    private String[] slopeAlbedos;
}
