package com.faforever.neroxis.util.jsonb;

import com.faforever.neroxis.util.vector.Vector3;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.JsonAdapter;
import io.avaje.jsonb.JsonReader;
import io.avaje.jsonb.JsonWriter;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.spi.PropertyNames;

@CustomAdapter
public final class Vector3Adapter implements JsonAdapter<Vector3> {

  private final JsonAdapter<Float> pfloatJsonAdapter;
  private final PropertyNames names;

  public Vector3Adapter(Jsonb jsonb) {
    this.pfloatJsonAdapter = jsonb.adapter(Float.TYPE);
    this.names = jsonb.properties("x", "y", "z");
  }

  @Override
  public void toJson(JsonWriter writer, Vector3 vector3) {
    writer.beginObject(names);
    writer.name(0);
    pfloatJsonAdapter.toJson(writer, vector3.getX());
    writer.name(1);
    pfloatJsonAdapter.toJson(writer, vector3.getY());
    writer.name(2);
    pfloatJsonAdapter.toJson(writer, vector3.getZ());
    writer.endObject();
  }

  @Override
  public Vector3 fromJson(JsonReader reader) {
    float      _val$x = 0;
    float      _val$y = 0;
    float      _val$z = 0;

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

        default:
          reader.unmappedField(fieldName);
          reader.skipValue();
      }
    }
    reader.endObject();

    // build and return Vector3
    Vector3 _$vector3 = new Vector3(_val$x, _val$y, _val$z);
    return _$vector3;
  }
}
