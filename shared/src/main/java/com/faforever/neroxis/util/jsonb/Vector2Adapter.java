package com.faforever.neroxis.util.jsonb;

import com.faforever.neroxis.util.vector.Vector2;
import io.avaje.jsonb.CustomAdapter;
import io.avaje.jsonb.JsonAdapter;
import io.avaje.jsonb.JsonReader;
import io.avaje.jsonb.JsonWriter;
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.spi.PropertyNames;

@CustomAdapter
public final class Vector2Adapter implements JsonAdapter<Vector2> {

  private final JsonAdapter<Float> pfloatJsonAdapter;
  private final PropertyNames names;

  public Vector2Adapter(Jsonb jsonb) {
    this.pfloatJsonAdapter = jsonb.adapter(Float.TYPE);
    this.names = jsonb.properties("x", "y");
  }

  @Override
  public void toJson(JsonWriter writer, Vector2 vector2) {
    writer.beginObject(names);
    writer.name(0);
    pfloatJsonAdapter.toJson(writer, vector2.getX());
    writer.name(1);
    pfloatJsonAdapter.toJson(writer, vector2.getY());
    writer.endObject();
  }

  @Override
  public Vector2 fromJson(JsonReader reader) {
    float      _val$x = 0;
    float      _val$y = 0;

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

        default:
          reader.unmappedField(fieldName);
          reader.skipValue();
      }
    }
    reader.endObject();

    Vector2 _$vector2 = new Vector2(_val$x, _val$y);
    return _$vector2;
  }
}
