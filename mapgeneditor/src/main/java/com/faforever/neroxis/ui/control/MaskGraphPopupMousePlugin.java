package com.faforever.neroxis.ui.control;

import com.faforever.neroxis.generator.graph.domain.MapMaskMethodVertex;
import com.faforever.neroxis.generator.graph.domain.MaskConstructorVertex;
import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.components.JScrollMenu;
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
import java.util.Arrays;
import java.util.List;
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
        final GraphElementAccessor<MaskGraphVertex<?>, MaskMethodEdge> pickSupport = vv.getPickSupport();
        final Set<MaskGraphVertex<?>> selectedVertices = vv.getSelectedVertices();

        final MaskGraphVertex<?> selected;

        if (selectedVertices.size() == 1) {
            selected = selectedVertices.stream().findFirst().orElse(null);
        } else {
            selected = null;
        }

        JPopupMenu popup = new JPopupMenu();
        if (pickSupport != null) {

            final MaskGraphVertex<?> picked = pickSupport.getVertex(layoutModel, lp);
            final MaskMethodEdge edge = pickSupport.getEdge(layoutModel, lp);
            final MutableSelectedState<MaskGraphVertex<?>> pickedVertexState = vv.getSelectedVertexState();
            final MutableSelectedState<MaskMethodEdge> pickedEdgeState = vv.getSelectedEdgeState();

            if (picked != null) {
                popup.add(
                        new AbstractAction("Edit Vertex") {
                            public void actionPerformed(ActionEvent e) {
                                pickedVertexState.clear();
                                pickedVertexState.select(picked);
                                redrawGraph(vv);
                            }
                        });
                popup.add(
                        new AbstractAction("Delete Vertex") {
                            public void actionPerformed(ActionEvent e) {
                                pickedVertexState.deselect(picked);
                                graph.removeVertex(picked);
                                redrawGraph(vv);
                            }
                        });

                if (selected != null && selected != picked) {
                    JMenu parameterMenu = new JMenu("Set as Parameter");
                    List<String> resultNames = picked.getResultNames();
                    if (!resultNames.isEmpty()) {
                        resultNames.forEach(resultName -> {
                            JMenu resultMenu = new JMenu(resultName);
                            Arrays.stream(selected.getExecutable().getParameters())
                                    .filter(parameter -> MaskReflectUtil.getActualTypeClass(selected.getExecutorClass(), parameter.getParameterizedType()).isAssignableFrom(picked.getResultClass(resultName)))
                                    .forEach(parameter -> resultMenu.add(
                                            new AbstractAction(parameter.getName()) {
                                                public void actionPerformed(ActionEvent e) {
                                                    graph.addEdge(picked, selected, new MaskMethodEdge(resultName, parameter.getName()));
                                                    redrawGraph(vv);
                                                }
                                            }));

                            if (selected instanceof MaskMethodVertex
                                    && Objects.equals(picked.getResultClass(resultName), selected.getExecutorClass())
                                    && graph.outgoingEdgesOf(picked).stream().noneMatch(outEdge -> MaskMethodVertex.EXECUTOR.equals(outEdge.getParameterName()) && resultName.equals(outEdge.getResultName()))
                                    && graph.incomingEdgesOf(selected).stream().noneMatch(inEdge -> MaskMethodVertex.EXECUTOR.equals(inEdge.getParameterName()))
                            ) {
                                resultMenu.add(
                                        new AbstractAction(MaskMethodVertex.EXECUTOR) {
                                            public void actionPerformed(ActionEvent e) {
                                                graph.addEdge(picked, selected, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                                                redrawGraph(vv);
                                            }
                                        });
                            }

                            if (resultMenu.getItemCount() > 0) {
                                parameterMenu.add(resultMenu);
                            }
                        });
                    }

                    if (parameterMenu.getItemCount() > 0) {
                        popup.add(parameterMenu);
                    }
                }
            } else if (edge != null) {
                popup.add(
                        new AbstractAction("Remove Parameter") {
                            public void actionPerformed(ActionEvent e) {
                                pickedEdgeState.deselect(edge);
                                graph.removeEdge(edge);
                                redrawGraph(vv);
                            }
                        });
            }
        }
        JMenu constructorMenu = new JMenu("Create New Mask");
        MaskReflectUtil.getMaskClasses().forEach(maskClass -> constructorMenu.add(
                new AbstractAction(maskClass.getSimpleName()) {
                    public void actionPerformed(ActionEvent e) {
                        MaskConstructorVertex newVertex = new MaskConstructorVertex(MaskReflectUtil.getMaskConstructor(maskClass));
                        newVertex.setIdentifier(String.valueOf(newVertex.hashCode()));
                        graph.addVertex(newVertex);
                        addNewVertexToVisualization(newVertex, vv, p);
                    }
                }));

        if (constructorMenu.getItemCount() > 0) {
            popup.add(constructorMenu);
        }

        if (selected != null) {
            List<String> resultNames = selected.getResultNames();

            JMenu transformationMenu = new JMenu("Add Transformation");
            JMenu insertionMenu = new JMenu("Insert Transformation");

            resultNames.forEach(resultName -> {
                MaskMethodEdge executorEdge = graph.outgoingEdgesOf(selected).stream()
                        .filter(edge -> MaskMethodVertex.EXECUTOR.equals(edge.getParameterName()) && resultName.equals(edge.getResultName()))
                        .findFirst()
                        .orElse(null);

                Class<? extends Mask<?, ?>> selectedResultClass = selected.getResultClass(resultName);
                if (executorEdge == null) {
                    JScrollMenu resultMenu = new JScrollMenu(resultName);
                    MaskReflectUtil.getMaskMethods(selectedResultClass)
                            .forEach(method -> {
                                if (method.getDeclaringClass().equals(MapMaskMethods.class)) {
                                    resultMenu.add(
                                            new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                                                public void actionPerformed(ActionEvent e) {
                                                    MapMaskMethodVertex newVertex = new MapMaskMethodVertex(method);
                                                    graph.addVertex(newVertex);
                                                    graph.addEdge(selected, newVertex, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                                                    addNewVertexToVisualization(newVertex, vv, p);
                                                }
                                            });
                                } else {
                                    resultMenu.add(
                                            new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                                                public void actionPerformed(ActionEvent e) {
                                                    MaskMethodVertex newVertex = new MaskMethodVertex(method, selectedResultClass);
                                                    graph.addVertex(newVertex);
                                                    graph.addEdge(selected, newVertex, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                                                    addNewVertexToVisualization(newVertex, vv, p);
                                                }
                                            });
                                }
                            });
                    if (resultMenu.getItemCount() > 0) {
                        transformationMenu.add(resultMenu);
                    }
                } else {
                    MaskGraphVertex<?> target = graph.getEdgeTarget(executorEdge);
                    JScrollMenu resultMenu = new JScrollMenu(resultName);
                    MaskReflectUtil.getMaskMethods(selectedResultClass)
                            .stream().filter(method -> target.getExecutorClass().isAssignableFrom(MaskReflectUtil.getActualTypeClass(selectedResultClass, method.getGenericReturnType())))
                            .forEach(method -> {
                                if (method.getDeclaringClass().equals(MapMaskMethods.class)) {
                                    resultMenu.add(
                                            new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                                                public void actionPerformed(ActionEvent e) {
                                                    MapMaskMethodVertex newVertex = new MapMaskMethodVertex(method);
                                                    String targetIdentifier = target.getIdentifier();
                                                    newVertex.setIdentifier(targetIdentifier == null ? String.valueOf(newVertex.hashCode()) : targetIdentifier);
                                                    graph.addVertex(newVertex);
                                                    graph.removeEdge(executorEdge);
                                                    graph.addEdge(selected, newVertex, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                                                    graph.addEdge(newVertex, target, new MaskMethodEdge(MaskGraphVertex.SELF, MaskMethodVertex.EXECUTOR));
                                                    addNewVertexToVisualization(newVertex, vv, p);
                                                }
                                            });
                                } else {
                                    resultMenu.add(
                                            new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                                                public void actionPerformed(ActionEvent e) {
                                                    MaskMethodVertex newVertex = new MaskMethodVertex(method, selectedResultClass);
                                                    String targetIdentifier = target.getIdentifier();
                                                    newVertex.setIdentifier(targetIdentifier == null ? String.valueOf(newVertex.hashCode()) : targetIdentifier);
                                                    graph.addVertex(newVertex);
                                                    graph.removeEdge(executorEdge);
                                                    graph.addEdge(selected, newVertex, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                                                    graph.addEdge(newVertex, target, new MaskMethodEdge(MaskGraphVertex.SELF, MaskMethodVertex.EXECUTOR));
                                                    addNewVertexToVisualization(newVertex, vv, p);
                                                }
                                            });
                                }
                            });
                    if (resultMenu.getItemCount() > 0) {
                        insertionMenu.add(resultMenu);
                    }
                }
            });

            if (transformationMenu.getItemCount() > 0) {
                popup.add(transformationMenu);
            }

            if (insertionMenu.getItemCount() > 0) {
                popup.add(insertionMenu);
            }

        }
        if (popup.getComponentCount() > 0) {
            popup.show(vv.getComponent(), e.getX(), e.getY());
        }
    }

    private void redrawGraph(VisualizationViewer<MaskGraphVertex<?>, MaskMethodEdge> vv) {
        vv.getEdgeSpatial().recalculate();
        vv.getVertexSpatial().recalculate();
        vv.repaint();
        vv.fireStateChanged();
    }

    private void addNewVertexToVisualization(MaskGraphVertex<?> newVertex, VisualizationViewer<MaskGraphVertex<?>, MaskMethodEdge> vv, Point2D p) {
        Point2D p2d = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(p);
        vv.getVisualizationModel().getLayoutModel().set(newVertex, p2d.getX(), p2d.getY());
        redrawGraph(vv);
    }
}
