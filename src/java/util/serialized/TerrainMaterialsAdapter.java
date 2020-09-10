package util.serialized;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import map.TerrainMaterials;

import java.awt.*;
import java.io.IOException;

public strictfp class TerrainMaterialsAdapter extends TypeAdapter<TerrainMaterials> {
    @Override
    public TerrainMaterials read(JsonReader reader) throws IOException {
        TerrainMaterials terrainMaterials = new TerrainMaterials();
        reader.beginObject();
        String fieldname = null;

        while (reader.hasNext()) {
            JsonToken token = reader.peek();

            if (token.equals(JsonToken.NAME)) {
                fieldname = reader.nextName();
            }

            if ("name".equals(fieldname)) {
                token = reader.peek();
                if (token.equals(JsonToken.STRING)) {
                    terrainMaterials.setName(reader.nextString());
                }
            }

            if ("texturePaths".equals(fieldname)) {
                token = reader.peek();
                if (token.equals(JsonToken.BEGIN_ARRAY)) {
                    reader.beginArray();
                    String[] texturePaths = new String[TerrainMaterials.TERRAIN_TEXTURE_COUNT];
                    for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
                        token = reader.peek();
                        if (token.equals(JsonToken.STRING)) {
                            texturePaths[i] = reader.nextString();
                        }
                    }
                    reader.endArray();
                    terrainMaterials.setTexturePaths(texturePaths);
                }
            }

            if ("textureScales".equals(fieldname)) {
                token = reader.peek();
                if (token.equals(JsonToken.BEGIN_ARRAY)) {
                    reader.beginArray();
                    float[] textureScales = new float[TerrainMaterials.TERRAIN_TEXTURE_COUNT];
                    for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
                        token = reader.peek();
                        if (token.equals(JsonToken.NUMBER)) {
                            textureScales[i] = (float) reader.nextDouble();
                        }
                    }
                    reader.endArray();
                    terrainMaterials.setTextureScales(textureScales);
                }
            }

            if ("normalPaths".equals(fieldname)) {
                token = reader.peek();
                if (token.equals(JsonToken.BEGIN_ARRAY)) {
                    reader.beginArray();
                    String[] normalPaths = new String[TerrainMaterials.TERRAIN_NORMAL_COUNT];
                    for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
                        token = reader.peek();
                        if (token.equals(JsonToken.STRING)) {
                            normalPaths[i] = reader.nextString();
                        }
                    }
                    reader.endArray();
                    terrainMaterials.setNormalPaths(normalPaths);
                }
            }

            if ("normalScales".equals(fieldname)) {
                token = reader.peek();
                if (token.equals(JsonToken.BEGIN_ARRAY)) {
                    reader.beginArray();
                    float[] normalScales = new float[TerrainMaterials.TERRAIN_NORMAL_COUNT];
                    for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
                        token = reader.peek();
                        if (token.equals(JsonToken.NUMBER)) {
                            normalScales[i] = (float) reader.nextDouble();
                        }
                    }
                    reader.endArray();
                    terrainMaterials.setNormalScales(normalScales);
                }
            }

            if ("previewColors".equals(fieldname)) {
                token = reader.peek();
                if (token.equals(JsonToken.BEGIN_ARRAY)) {
                    reader.beginArray();
                    Color[] previewColors = new Color[TerrainMaterials.TERRAIN_NORMAL_COUNT];
                    for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
                        token = reader.peek();
                        if (token.equals(JsonToken.BEGIN_OBJECT)) {
                            reader.beginObject();
                            int value = 0;
                            float falpha = 0;
                            token = reader.peek();
                            if (token.equals(JsonToken.NAME)) {
                                fieldname = reader.nextName();
                                if (fieldname.equals("value")) {
                                    value = reader.nextInt();
                                }
                                fieldname = reader.nextName();
                                if (fieldname.equals("falpha")) {
                                    falpha = (float) reader.nextDouble();
                                }
                            }
                            previewColors[i] = new Color(value, falpha > 0);
                            reader.endObject();
                        } else if (token.equals(JsonToken.NULL)) {
                            reader.nextNull();
                            previewColors[i] = null;
                        }
                    }
                    reader.endArray();
                    terrainMaterials.setPreviewColors(previewColors);
                }
            }
        }
        reader.endObject();
        return terrainMaterials;
    }

    @Override
    public void write(JsonWriter writer, TerrainMaterials terrainMaterials) throws IOException {
        writer.beginObject();
        writer.name("name");
        writer.value(terrainMaterials.getName());
        writer.name("texturePaths");
        writer.beginArray();
        for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
            writer.value(terrainMaterials.getTexturePaths()[i]);
        }
        writer.endArray();
        writer.name("textureScales");
        writer.beginArray();
        for (int i = 0; i < TerrainMaterials.TERRAIN_TEXTURE_COUNT; i++) {
            writer.value(terrainMaterials.getTextureScales()[i]);
        }
        writer.endArray();
        writer.name("normalPaths");
        writer.beginArray();
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            writer.value(terrainMaterials.getNormalPaths()[i]);
        }
        writer.endArray();
        writer.name("normalScales");
        writer.beginArray();
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            writer.value(terrainMaterials.getNormalScales()[i]);
        }
        writer.endArray();
        writer.name("previewColors");
        writer.beginArray();
        for (int i = 0; i < TerrainMaterials.TERRAIN_NORMAL_COUNT; i++) {
            Color color = terrainMaterials.getPreviewColors()[i];
            if (color != null) {
                writer.beginObject();
                writer.name("value");
                writer.value(color.getRGB());
                writer.name("falpha");
                writer.value((double) 0);
                writer.endObject();
            } else {
                writer.nullValue();
            }
        }
        writer.endArray();
        writer.endObject();
    }
}