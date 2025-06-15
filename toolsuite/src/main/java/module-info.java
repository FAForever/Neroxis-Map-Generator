module com.faforever.neroxis.toolsuite {
    requires info.picocli;
    requires com.faforever.neroxis.shared;

    requires static lombok;

    opens com.faforever.neroxis.toolsuite to info.picocli;
    opens com.faforever.neroxis.toolsuite.cli to info.picocli;
}