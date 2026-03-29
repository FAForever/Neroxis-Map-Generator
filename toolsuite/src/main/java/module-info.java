import org.jspecify.annotations.NullMarked;

@NullMarked
module com.faforever.neroxis.toolsuite {
    requires info.picocli;
    requires com.faforever.neroxis.shared;

    requires static lombok;
    requires org.jspecify;

    opens com.faforever.neroxis.toolsuite to info.picocli;
    opens com.faforever.neroxis.toolsuite.cli to info.picocli;
}