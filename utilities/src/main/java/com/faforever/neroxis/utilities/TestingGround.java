package com.faforever.neroxis.utilities;

import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.io.GraphSerializationUtil;
import com.faforever.neroxis.util.DebugUtil;
import java.io.IOException;
import java.nio.file.Path;
import org.jgrapht.graph.DirectedAcyclicGraph;

public strictfp class TestingGround {
    public static void main(String[] args) throws Exception {
        DebugUtil.DEBUG = true;

        DebugUtil.timedRun(() -> {
            try {
                GraphSerializationUtil.importGraph(new DirectedAcyclicGraph<>(MaskMethodEdge.class),
                                                   Path.of("C:\\Users\\corey\\Documents\\basicTerrain.json").toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
