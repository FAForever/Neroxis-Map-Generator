import org.jspecify.annotations.NullMarked;

@NullMarked
module com.faforever.neroxis.generator {
    requires info.picocli;
    requires com.faforever.neroxis.shared;
    requires org.apache.commons.codec;

    requires static lombok;
    requires org.jspecify;
    requires io.avaje.jsonb;

    provides io.avaje.jsonb.spi.JsonbExtension with com.faforever.neroxis.generator.jsonb.GeneratedJsonComponent;

    opens com.faforever.neroxis.generator to info.picocli;
    opens com.faforever.neroxis.generator.cli to info.picocli;
}