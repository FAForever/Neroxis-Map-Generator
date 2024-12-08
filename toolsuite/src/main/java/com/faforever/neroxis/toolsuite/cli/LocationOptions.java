package com.faforever.neroxis.toolsuite.cli;

import com.faforever.neroxis.util.vector.Vector;
import com.faforever.neroxis.util.vector.Vector2;
import lombok.Getter;
import picocli.CommandLine;

@Getter
public class LocationOptions {
    private Vector2 location = new Vector2();

    @CommandLine.Option(names = "--x", required = true, description = "x-coordinate")
    public void setX(float x) {
        location = location.withComponent(Vector.X, x);
    }

    @CommandLine.Option(names = "--y", required = true, description = "y-coordinate")
    public void setY(float y) {
        location = location.withComponent(Vector.Y, y);
    }
}
