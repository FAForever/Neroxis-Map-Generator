package com.faforever.neroxis.generator.cli;


import com.faforever.neroxis.generator.PropStyle;
import com.faforever.neroxis.generator.ResourceStyle;
import com.faforever.neroxis.generator.TerrainStyle;
import com.faforever.neroxis.generator.TextureStyle;
import lombok.Getter;
import picocli.CommandLine;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
public class CustomStyleOptions {
    @Spec
    CommandLine.Model.CommandSpec spec;
    private TextureStyle textureStyle;
    private TerrainStyle terrainStyle;
    private ResourceStyle resourceStyle;
    private PropStyle propStyle;

    @Option(names = "--texture-style", description = "Texture style to use for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setTextureStyle(TextureStyle textureStyle) {
        this.textureStyle = textureStyle;
    }

    @Option(names = "--terrain-style", order = 29, description = "Terrain style to use for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setTerrainStyle(TerrainStyle terrainStyle) {
        this.terrainStyle = terrainStyle;
    }

    @Option(names = "--resource-style", order = 29, description = "Resource style to use for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setResourceStyle(ResourceStyle resourceStyle) {
        this.resourceStyle = resourceStyle;
    }

    @Option(names = "--prop-style", order = 29, description = "Prop style to use for the generated map. Values: ${COMPLETION-CANDIDATES}")
    public void setPropStyle(PropStyle propStyle) {
        this.propStyle = propStyle;
    }
}
