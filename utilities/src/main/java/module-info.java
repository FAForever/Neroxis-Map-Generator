import org.jspecify.annotations.NullMarked;

@NullMarked
module com.faforever.neroxis.utilities {
    requires com.faforever.neroxis.shared;
    requires org.jspecify;

    opens com.faforever.neroxis.utilities to info.picocli;
}