package map;

public strictfp class Material {
    static String pathFormat = "/env/%s/layers/%s_%s.dds";

    public final String texturePath;
    public final String normalPath;
    public final float textureScale;
    public final float normalScale;

    public Material(String environment, String name, float scale)  {
        this(environment, name, name, scale);
    }

    public Material(String environment, String texture, String normal, float scale){
        this(environment, texture, normal, scale, scale);
    }
    public Material(String environment, String texture, String normal, float textureScale, float normalScale){
        texturePath = String.format(pathFormat, environment, texture, "albedo");
        normalPath = String.format(pathFormat, environment, normal, "normals");
        this.textureScale= textureScale;
        this.normalScale=normalScale;
    }

}