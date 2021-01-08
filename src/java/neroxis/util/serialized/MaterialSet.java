package neroxis.util.serialized;

import lombok.Data;

import java.util.List;

/**
 * Used in disk operations to be converted into a material later
 */

@Data
public strictfp class MaterialSet {
    private String name;
    private List<Material> materials;
    private MaterialElement macroTexture;

    @Data
    public static strictfp class Material {
        private MaterialElement texture;
        private MaterialElement normal;
        private PreviewColor previewColor;
    }

    @Data
    public static strictfp class MaterialElement {
        private String environment;
        private String name;
        private float scale;
    }

    @Data
    public static strictfp class PreviewColor {
        private int red;
        private int green;
        private int blue;
    }
}