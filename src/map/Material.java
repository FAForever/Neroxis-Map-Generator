package map;

public strictfp class Material {
    static String pathFormat = "/env/%s/layers/%s_%s.dds";

    public final String texturePath;
    public final String normalPath;
    public final float scale;

    public Material(String environment, String name, float scale){
        texturePath = String.format(pathFormat, environment, name, "albedo");
        normalPath = String.format(pathFormat, environment, name, "normals");
        this.scale= scale;
    }

}
