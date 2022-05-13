package com.faforever.neroxis.utilities;

import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.io.GraphSerializationUtil;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.MaskGraphReflectUtil;
import java.io.FileInputStream;
import java.nio.file.Path;
import org.jgrapht.graph.DirectedAcyclicGraph;

public strictfp class TestingGround {
    public static void main(String[] args) throws Exception {
        DebugUtil.DEBUG = true;

        DebugUtil.timedRun(() -> MaskGraphReflectUtil.classIsNumeric(int.class));

        for (int i = 0; i < 1; i++) {
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> jsonGraph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            GraphSerializationUtil.importGraphJson(jsonGraph, new FileInputStream(Path.of("tmp.json").toFile()));

            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> dotGraph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            GraphSerializationUtil.importGraphDot(dotGraph, new FileInputStream(Path.of("tmp.dot").toFile()));
        }
    }
}
