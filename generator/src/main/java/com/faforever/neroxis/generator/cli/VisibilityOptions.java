package com.faforever.neroxis.generator.cli;

import lombok.Getter;
import picocli.CommandLine;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
@SuppressWarnings("unused")
public strictfp class VisibilityOptions {
    @Spec
    CommandLine.Model.CommandSpec spec;

    private boolean tournamentStyle;
    private boolean blind;
    private boolean unexplored;

    @Option(names = "--tournament-style", description = "Remove the preview.png and add time of original generation to map")
    public void setTournamentStyle(boolean value) {
        this.tournamentStyle = value;
    }

    @Option(names = "--blind", description = "Remove the preview.png, add time of original generation to map, and remove in game lobby preview")
    public void setBlind(boolean value) {
        this.tournamentStyle = value;
        this.blind = value;
    }

    @Option(names = "--unexplored", description = "Remove the preview.png, add time of original generation to map, remove in game lobby preview, and add unexplored fog of war")
    public void setUnexplored(boolean value) {
        this.tournamentStyle = value;
        this.blind = value;
        this.unexplored = value;
    }
}
