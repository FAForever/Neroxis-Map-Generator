import org.jspecify.annotations.NullMarked;

@NullMarked
module com.faforever.neroxis.shared {
    exports com.faforever.neroxis.cli;
    exports com.faforever.neroxis.util.vector;
    exports com.faforever.neroxis.map;
    exports com.faforever.neroxis.mask;
    exports com.faforever.neroxis.exporter;
    exports com.faforever.neroxis.importer;
    exports com.faforever.neroxis.util;
    exports com.faforever.neroxis.biomes;
    exports com.faforever.neroxis.map.placement;
    exports com.faforever.neroxis.util.serial.biome;
    exports com.faforever.neroxis.brushes;
    exports com.faforever.neroxis.bases;
    exports com.faforever.neroxis.util.functional;

    requires io.avaje.jsonb;
    requires org.antlr.antlr4.runtime;
    requires info.picocli;

    requires transitive java.desktop;

    requires static lombok;
    requires org.jspecify;

    provides io.avaje.jsonb.spi.JsonbExtension with com.faforever.neroxis.jsonb.GeneratedJsonComponent;

    opens com.faforever.neroxis.cli to info.picocli;
}