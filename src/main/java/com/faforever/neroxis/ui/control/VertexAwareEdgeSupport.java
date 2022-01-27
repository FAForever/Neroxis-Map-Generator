package com.faforever.neroxis.ui.control;

import org.jgrapht.Graph;
import org.jungrapht.visualization.VisualizationServer;
import org.jungrapht.visualization.control.CubicCurveEdgeEffects;
import org.jungrapht.visualization.control.EdgeEffects;
import org.jungrapht.visualization.control.EdgeSupport;

import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.function.BiFunction;

public class VertexAwareEdgeSupport<V, E> implements EdgeSupport<V, E> {

    protected Point2D down;
    protected EdgeEffects<V, E> edgeEffects;
    protected BiFunction<V, V, E> edgeFactory;
    protected V startVertex;

    public VertexAwareEdgeSupport(BiFunction<V, V, E> edgeFactory) {
        this.edgeFactory = edgeFactory;
        this.edgeEffects = new CubicCurveEdgeEffects<>();
    }

    @Override
    public void startEdgeCreate(VisualizationServer<V, E> vv, V startVertex, Point2D startPoint) {
        this.startVertex = startVertex;
        this.down = startPoint;
        this.edgeEffects.startEdgeEffects(vv, startPoint, startPoint);
        if (vv.getVisualizationModel().getGraph().getType().isDirected()) {
            this.edgeEffects.startArrowEffects(vv, startPoint, startPoint);
        }
        vv.repaint();
    }

    @Override
    public void midEdgeCreate(VisualizationServer<V, E> vv, Point2D midPoint) {
        if (startVertex != null) {
            this.edgeEffects.midEdgeEffects(vv, down, midPoint);
            if (vv.getVisualizationModel().getGraph().getType().isDirected()) {
                this.edgeEffects.midArrowEffects(vv, down, midPoint);
            }
            vv.repaint();
        }
    }

    @Override
    public void endEdgeCreate(VisualizationServer<V, E> vv, V endVertex) {
        Objects.requireNonNull(vv.getVisualizationModel().getGraph(), "graph must be non-null");
        if (startVertex != null) {
            Graph<V, E> graph = vv.getVisualizationModel().getGraph();
            graph.addEdge(startVertex, endVertex, edgeFactory.apply(startVertex, endVertex));
            vv.getEdgeSpatial().recalculate();
        }
        startVertex = null;
        edgeEffects.endEdgeEffects(vv);
        edgeEffects.endArrowEffects(vv);
        vv.repaint();
    }

    @Override
    public void abort(VisualizationServer<V, E> vv) {
        startVertex = null;
        edgeEffects.endEdgeEffects(vv);
        edgeEffects.endArrowEffects(vv);
        vv.repaint();
    }

    public EdgeEffects<V, E> getEdgeEffects() {
        return edgeEffects;
    }

    public void setEdgeEffects(EdgeEffects<V, E> edgeEffects) {
        this.edgeEffects = edgeEffects;
    }

    public BiFunction<V, V, E> getEdgeFactory() {
        return edgeFactory;
    }

    public void setEdgeFactory(BiFunction<V, V, E> edgeFactory) {
        this.edgeFactory = edgeFactory;
    }
}
