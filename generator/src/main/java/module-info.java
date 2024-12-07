module com.faforever.neroxis.generator {
    requires com.faforever.neroxis.shared;
    requires org.apache.commons.codec;
    requires static lombok;

    opens com.faforever.neroxis.generator to info.picocli;
    opens com.faforever.neroxis.generator.cli to info.picocli;
}