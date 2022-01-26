package com.faforever.neroxis.ui.control;

import com.faforever.neroxis.graph.domain.MaskConstructorVertex;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.util.MaskReflectUtil;
import org.jgrapht.Graph;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.AbstractPopupGraphMousePlugin;
import org.jungrapht.visualization.control.GraphElementAccessor;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.selection.MutableSelectedState;
import org.jungrapht.visualization.util.PointUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * a plugin that uses popup menus to create vertices, undirected edges, and directed edges.
 */
public class MaskGraphPopupMousePlugin extends AbstractPopupGraphMousePlugin {

    protected void handlePopup(MouseEvent e) {
        final VisualizationViewer<MaskGraphVertex<?>, MaskMethodEdge> vv = (VisualizationViewer<MaskGraphVertex<?>, MaskMethodEdge>) e.getSource();
        final LayoutModel<MaskGraphVertex<?>> layoutModel = vv.getVisualizationModel().getLayoutModel();

        final Graph<MaskGraphVertex<?>, MaskMethodEdge> graph = vv.getVisualizationModel().getGraph();
        final Point2D p = e.getPoint();
        final Point lp =
                PointUtils.convert(vv.getRenderContext().getMultiLayerTransformer().inverseTransform(p));
        GraphElementAccessor<MaskGraphVertex<?>, MaskMethodEdge> pickSupport = vv.getPickSupport();
        if (pickSupport != null) {

            final MaskGraphVertex<?> vertex = pickSupport.getVertex(layoutModel, lp);
            final MaskMethodEdge edge = pickSupport.getEdge(layoutModel, lp);
            final MutableSelectedState<MaskGraphVertex<?>> pickedVertexState = vv.getSelectedVertexState();
            final MutableSelectedState<MaskMethodEdge> pickedEdgeState = vv.getSelectedEdgeState();

            JPopupMenu popup = new JPopupMenu();
            if (vertex != null) {
                popup.add(
                        new AbstractAction("Edit Vertex") {
                            public void actionPerformed(ActionEvent e) {
                                pickedVertexState.clear();
                                pickedVertexState.select(vertex);
                                vv.repaint();
                            }
                        });
                Set<MaskGraphVertex<?>> picked = vv.getSelectedVertices();
                if (picked.size() == 1) {
                    JMenu menu = new JMenu("Set as Parameter");
                    picked.stream().findFirst().ifPresent(other -> {
                        if (other == vertex) {
                            return;
                        }

                        Executable executable = other.getExecutable();
                        if (executable != null) {
                            Arrays.stream(executable.getParameters())
                                    .filter(parameter -> other.getParameter(parameter) == null)
                                    .filter(parameter -> MaskReflectUtil.getActualParameterClass(other.getExecutorClass(), parameter).isAssignableFrom(vertex.getResultClass()))
                                    .forEach(parameter -> menu.add(
                                            new AbstractAction(parameter.getName()) {
                                                public void actionPerformed(ActionEvent e) {
                                                    graph.addEdge(vertex, other, new MaskMethodEdge(parameter.getName()));
                                                    vv.repaint();
                                                    vv.fireStateChanged();
                                                }
                                            }));
                        }

                        if (other instanceof MaskMethodVertex
                                && Objects.equals(vertex.getResultClass(), other.getExecutorClass())
                                && graph.outgoingEdgesOf(vertex).stream().noneMatch(outEdge -> "executor".equals(outEdge.getParameterName()))
                        ) {
                            menu.add(
                                    new AbstractAction("executor") {
                                        public void actionPerformed(ActionEvent e) {
                                            graph.addEdge(vertex, other, new MaskMethodEdge("executor"));
                                            vv.repaint();
                                            vv.fireStateChanged();
                                        }
                                    });
                        }
                    });
                    if (menu.getItemCount() > 0) {
                        popup.add(menu);
                    }
                }
                popup.add(
                        new AbstractAction("Delete Vertex") {
                            public void actionPerformed(ActionEvent e) {
                                pickedVertexState.deselect(vertex);
                                graph.removeVertex(vertex);
                                vv.getVertexSpatial().recalculate();
                                vv.repaint();
                                vv.fireStateChanged();
                            }
                        });
            } else if (edge != null) {
                popup.add(
                        new AbstractAction("Remove Parameter") {
                            public void actionPerformed(ActionEvent e) {
                                pickedEdgeState.deselect(edge);
                                graph.removeEdge(edge);
                                vv.getEdgeSpatial().recalculate();
                                vv.repaint();
                                vv.fireStateChanged();
                            }
                        });
            } else {
                JMenu constructorMenu = new JMenu("Create Constructor Vertex");
                popup.add(constructorMenu);
                MaskReflectUtil.getMaskClasses().forEach(maskClass -> constructorMenu.add(
                        new AbstractAction(maskClass.getSimpleName()) {
                            public void actionPerformed(ActionEvent e) {
                                MaskConstructorVertex<?> newVertex = new MaskConstructorVertex<>();
                                newVertex.setExecutorClass(maskClass);
                                graph.addVertex(newVertex);
                                Point2D p2d = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(p);
                                vv.getVisualizationModel().getLayoutModel().set(newVertex, p2d.getX(), p2d.getY());
                                vv.getSelectedVertexState().clear();
                                vv.getSelectedVertexState().select(newVertex);
                                vv.repaint();
                                vv.fireStateChanged();
                            }
                        }));

                JMenu methodMenu = new JMenu("Create Method Vertex");
                popup.add(methodMenu);
                MaskReflectUtil.getMaskClasses().forEach(maskClass -> methodMenu.add(
                        new AbstractAction(maskClass.getSimpleName()) {
                            public void actionPerformed(ActionEvent e) {
                                MaskMethodVertex newVertex = new MaskMethodVertex();
                                newVertex.setExecutorClass(maskClass);
                                graph.addVertex(newVertex);
                                Point2D p2d = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(p);
                                vv.getVisualizationModel().getLayoutModel().set(newVertex, p2d.getX(), p2d.getY());
                                vv.getSelectedVertexState().clear();
                                vv.getSelectedVertexState().select(newVertex);
                                vv.repaint();
                                vv.fireStateChanged();
                            }
                        }));
            }
            if (popup.getComponentCount() > 0) {
                popup.show(vv.getComponent(), e.getX(), e.getY());
            }
        }
    }
}

