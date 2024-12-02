package com.faforever.neroxis.util.jsonb;

import com.faforever.neroxis.util.vector.Vector4;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.JsonAdapter;
import io.avaje.jsonb.JsonReader;
import io.avaje.jsonb.JsonWriter;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.spi.PropertyNames;

@CustomAdapter
public final class Vector4Adapter implements JsonAdapter<Vector4> {

  private final JsonAdapter<Float> pfloatJsonAdapter;
  private final PropertyNames names;

  public Vector4Adapter(Jsonb jsonb) {
    this.pfloatJsonAdapter = jsonb.adapter(Float.TYPE);
    this.names = jsonb.properties("x", "y", "z", "w");
  }

  @Override
  public void toJson(JsonWriter writer, Vector4 vector4) {
    writer.beginObject(names);
    writer.name(0);
    pfloatJsonAdapter.toJson(writer, vector4.getX());
    writer.name(1);
    pfloatJsonAdapter.toJson(writer, vector4.getY());
    writer.name(2);
    pfloatJsonAdapter.toJson(writer, vector4.getZ());
    writer.name(3);
    pfloatJsonAdapter.toJson(writer, vector4.getW());
    writer.endObject();
  }

  @Override
  public Vector4 fromJson(JsonReader reader) {
    float      _val$x = 0;
    float      _val$y = 0;
    float      _val$z = 0;
    float      _val$w = 0;

    // read json
    reader.beginObject(names);
    while (reader.hasNextField()) {
      final String fieldName = reader.nextField();
      switch (fieldName) {
        case "x": 
          _val$x = pfloatJsonAdapter.fromJson(reader);
          break;

        case "y": 
          _val$y = pfloatJsonAdapter.fromJson(reader);
          break;

        case "z": 
          _val$z = pfloatJsonAdapter.fromJson(reader);
          break;

        case "w": 
          _val$w = pfloatJsonAdapter.fromJson(reader);
          break;

        default:
          reader.unmappedField(fieldName);
          reader.skipValue();
      }
    }
    reader.endObject();

    // build and return Vector4
    Vector4 _$vector4 = new Vector4(_val$x, _val$y, _val$z, _val$w);
    return _$vector4;
  }
}
