package map;

import lombok.Getter;

@Getter
public strictfp class Material {
    private static String texturePathFormat = "/env/%s/layers/%s_albedo.dds";
    private static String normalPathFormat = "/env/%s/layers/%s.dds";

    private final String texturePath;
    private final String normalPath;
    private final float textureScale;
    private final float normalScale;

    public Material(String environment, String name, float scale)  {
        this(environment, name, name, scale);
    }

    public Material(String environment, String texture, String normal, float scale){
        this(environment, texture, normal, scale, scale);
    }
    public Material(String environment, String texture, String normal, float textureScale, float normalScale){
        this(environment, environment, texture, normal, textureScale, normalScale);
    }
    public Material(String texEnv, String normalEnv, String texture, String normal, float textureScale, float normalScale) {
        texturePath = String.format(texturePathFormat, texEnv, texture);
        normalPath = String.format(normalPathFormat, normalEnv, normal);
        this.textureScale= textureScale;
        this.normalScale=normalScale;
    }

}