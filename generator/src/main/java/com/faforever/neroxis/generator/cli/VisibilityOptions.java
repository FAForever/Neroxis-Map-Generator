package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.generator.Visibility;
import lombok.Getter;

import static picocli.CommandLine.Option;

@Getter
@SuppressWarnings("unused")
public class VisibilityOptions {
    @Option(names = "--visibility", order = 1, description = "Visibility for the generated map. Values: ${COMPLETION-CANDIDATES}")
    private Visibility visibility;

    @Option(names = "--tournament-style", order = 2, hidden = true, description = "Remove the preview.png and add time of original generation to map")
    private void setTournamentStyle(boolean value) {
        this.visibility = Visibility.TOURNAMENT;
    }

    @Option(names = "--blind", order = 3, hidden = true, description = "Remove the preview.png, add time of original generation to map, and remove in game lobby preview")
    private void setBlind(boolean value) {
        this.visibility = Visibility.BLIND;
    }

    @Option(names = "--unexplored", order = 4, hidden = true, description = "Remove the preview.png, add time of original generation to map, remove in game lobby preview, and add unexplored fog of war")
    private void setUnexplored(boolean value) {
        this.visibility = Visibility.UNEXPLORED;
    }
}
