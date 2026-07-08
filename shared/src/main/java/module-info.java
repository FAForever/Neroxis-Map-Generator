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

    requires org.antlr.antlr4.runtime;
    requires info.picocli;

    requires transitive java.desktop;

    requires static lombok;
    requires org.jspecify;
    requires com.fasterxml.jackson.annotation;
    requires tools.jackson.databind;

    opens com.faforever.neroxis.cli to info.picocli;
    opens com.faforever.neroxis.util.serial.biome to tools.jackson.databind;
}