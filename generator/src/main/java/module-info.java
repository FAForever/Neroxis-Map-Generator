import org.jspecify.annotations.NullMarked;

@NullMarked
module com.faforever.neroxis.generator {
    requires info.picocli;
    requires com.faforever.neroxis.shared;
    requires org.apache.commons.codec;

    requires static lombok;
    requires org.jspecify;
    requires com.fasterxml.jackson.annotation;

    opens com.faforever.neroxis.generator to info.picocli;
    opens com.faforever.neroxis.generator.cli to info.picocli;
    opens com.faforever.neroxis.generator.util.serial to info.picocli, tools.jackson.databind;
}