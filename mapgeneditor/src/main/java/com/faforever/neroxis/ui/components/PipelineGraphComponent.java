package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.generator.graph.domain.MapMaskMethodVertex;
import com.faforever.neroxis.generator.graph.domain.MaskConstructorVertex;
import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.handler.ConnectionHandler;
import com.faforever.neroxis.ngraph.swing.handler.Rubberband;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.util.MaskReflectUtil;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;

public class PipelineGraphComponent extends GraphComponent {
    private PipelineGraph pipelineGraph;

    public PipelineGraphComponent(PipelineGraph graph) {
        super(graph);
        setToolTips(true);
        setFoldingEnabled(false);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE) {
                    pipelineGraph.getSelectionCells().forEach(selectedCell -> {
                        if (selectedCell.isEdge()) {
                            ICell source = selectedCell.getSource().getParent();
                            ICell target = selectedCell.getTarget().getParent();
                            List<ICell> edges = pipelineGraph.getEdgesBetween(source, target);
                            edges.stream().map(edge -> pipelineGraph.getEdgeForCell(edge)).filter(Objects::nonNull).forEach(pipelineGraph::removeEdge);
                        } else if (selectedCell.isVertex()) {
                            MaskGraphVertex<?> vertex = pipelineGraph.getVertexForCell(selectedCell);
                            pipelineGraph.removeVertex(vertex);
                        }
                    });
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT && e.isControlDown()) {
                    ICell selectedCell = pipelineGraph.getSelectionCell();
                    if (selectedCell != null) {
                        MaskGraphVertex<?> vertex = pipelineGraph.getVertexForCell(selectedCell);
                        MaskGraphVertex<?> ancestor = pipelineGraph.getDirectAncestor(vertex);
                        if (ancestor != null) {
                            graph.setSelectionCell(pipelineGraph.getCellForVertex(ancestor));
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && e.isControlDown()) {
                    ICell selectedCell = pipelineGraph.getSelectionCell();
                    if (selectedCell != null) {
                        MaskGraphVertex<?> vertex = pipelineGraph.getVertexForCell(selectedCell);
                        MaskGraphVertex<?> descendant = pipelineGraph.getDirectDescendant(vertex);
                        if (descendant != null) {
                            graph.setSelectionCell(pipelineGraph.getCellForVertex(descendant));
                        }
                    }
                }
            }
        });

        graphControl.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    handlePopup(e);
                } else if (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown()) {
                    MaskGraphVertex<?> vertex = graph.getVertexForCell(getCellAt(e.getX(), e.getY()));
                    if (vertex != null) {
                        List<ICell> directRelationships = graph.getDirectRelationships(vertex).stream().map(graph::getCellForVertex).filter(Objects::nonNull).collect(Collectors.toList());
                        if (e.isShiftDown()) {
                            graph.addSelectionCells(directRelationships);
                        } else {
                            graph.setSelectionCells(directRelationships);
                        }
                        e.consume();
                    }
                }
            }
        });
        graphControl.addMouseListener(new Rubberband(this));
        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() < 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
                JViewport viewport = getViewport();
                Point point = viewport.getViewRect().getLocation();
                point.translate(e.getX(), e.getY());
                viewport.setViewPosition(point);
            }
        });
    }

    protected ConnectionHandler createConnectionHandler() {
        return new PipelineConnectionHandler(this);
    }

    protected TransferHandler createTransferHandler() {
        return new PipelineGraphTransferHandler(this);
    }

    public PipelineGraph getGraph() {
        return pipelineGraph;
    }

    public void setGraph(Graph graph) {
        if (!(graph instanceof PipelineGraph)) {
            throw new IllegalArgumentException("Graph is not a PipelineGraph");
        }
        pipelineGraph = (PipelineGraph) graph;
        super.setGraph(graph);
    }

    protected void installDoubleClickHandler() {
    }

    protected void handlePopup(MouseEvent mouseEvent) {
        ICell selectedCell = getGraph().getSelectionCell();
        final MaskGraphVertex<?> selected = pipelineGraph.getVertexForCell(selectedCell);
        JPopupMenu popup = new JPopupMenu();
        JMenu constructorMenu = new JMenu("Create New Mask");
        MaskReflectUtil.getMaskClasses().forEach(maskClass -> constructorMenu.add(new AbstractAction(maskClass.getSimpleName()) {
            public void actionPerformed(ActionEvent e) {
                MaskConstructorVertex newVertex = new MaskConstructorVertex(MaskReflectUtil.getMaskConstructor(maskClass));
                newVertex.setIdentifier(String.valueOf(newVertex.hashCode()));
                pipelineGraph.addVertex(newVertex);
                moveVertexToMouse(newVertex, mouseEvent);
                pipelineGraph.refresh();
                pipelineGraph.clearSelection();
                pipelineGraph.addSelectionCell(pipelineGraph.getCellForVertex(newVertex));
            }
        }));
        if (constructorMenu.getItemCount() > 0) {
            popup.add(constructorMenu);
        }
        JMenu methodMenu = new JMenu("Create New Method");
        MaskReflectUtil.getMaskClasses().forEach(maskClass -> {
            JMenu subMethodMenu = new JScrollMenu(maskClass.getSimpleName());
            subMethodMenu.addMenuKeyListener(new MenuKeyListener() {
                @Override
                public void menuKeyTyped(MenuKeyEvent e) {
                }

                @Override
                public void menuKeyPressed(MenuKeyEvent e) {
                    Arrays.stream(subMethodMenu.getMenuComponents())
                          .filter(component -> component instanceof JMenuItem)
                          .map(component -> (JMenuItem) component)
                          .filter(menuItem -> ((String) menuItem.getAction()
                                                                .getValue(Action.NAME)).startsWith(String.valueOf(e.getKeyChar())))
                          .findFirst()
                          .ifPresent(jMenuItem -> subMethodMenu.getPopupMenu().setSelected(jMenuItem));
                }

                @Override
                public void menuKeyReleased(MenuKeyEvent e) {
                }
            });
            MaskReflectUtil.getMaskMethods(maskClass)
                           .forEach(method -> subMethodMenu.add(new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                               public void actionPerformed(ActionEvent e) {
                                   MaskMethodVertex newVertex = new MaskMethodVertex(method, maskClass);
                                   pipelineGraph.addVertex(newVertex);
                                   moveVertexToMouse(newVertex, mouseEvent);
                                   pipelineGraph.refresh();
                                   pipelineGraph.clearSelection();
                                   pipelineGraph.addSelectionCell(pipelineGraph.getCellForVertex(newVertex));
                               }
                           }));
            methodMenu.add(subMethodMenu);
        });
        if (methodMenu.getItemCount() > 0) {
            popup.add(methodMenu);
        }
        if (selected != null) {
            List<String> resultNames = selected.getResultNames();
            JMenu transformationMenu = new JMenu("Add Transformation");
            JMenu insertionMenu = new JMenu("Insert Transformation");
            resultNames.forEach(resultName -> {
                MaskMethodEdge executorEdge = pipelineGraph.outgoingEdgesOf(selected).stream().filter(edge -> MaskMethodVertex.EXECUTOR.equals(edge.getParameterName()) && resultName.equals(edge.getResultName())).findFirst().orElse(null);
                Class<? extends Mask<?, ?>> selectedResultClass = selected.getResultClass(resultName);
                if (executorEdge == null) {
                    JScrollMenu resultMenu = buildTransformationSubMenu(mouseEvent, selected, resultName, selectedResultClass);
                    if (resultMenu.getItemCount() > 0) {
                        transformationMenu.add(resultMenu);
                    }
                } else {
                    MaskGraphVertex<?> target = pipelineGraph.getEdgeTarget(executorEdge);
                    JScrollMenu resultMenu = buildInsertionSubMenu(mouseEvent, selected, resultName, executorEdge, selectedResultClass, target);
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
            popup.show((Component) mouseEvent.getSource(), mouseEvent.getX(), mouseEvent.getY());
        }
    }

    private JScrollMenu buildInsertionSubMenu(MouseEvent mouseEvent, MaskGraphVertex<?> selected, String resultName, MaskMethodEdge executorEdge, Class<? extends Mask<?, ?>> selectedResultClass, MaskGraphVertex<?> target) {
        JScrollMenu resultMenu = new JScrollMenu(resultName);
        MaskReflectUtil.getMaskMethods(selectedResultClass).stream().filter(method -> target.getExecutorClass().isAssignableFrom(MaskReflectUtil.getActualTypeClass(selectedResultClass, method.getGenericReturnType()))).forEach(method -> {
            if (method.getDeclaringClass().equals(MapMaskMethods.class)) {
                resultMenu.add(new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                    public void actionPerformed(ActionEvent e) {
                        MapMaskMethodVertex newVertex = new MapMaskMethodVertex(method);
                        String targetIdentifier = target.getIdentifier();
                        newVertex.setIdentifier(targetIdentifier == null ? String.valueOf(newVertex.hashCode()) : targetIdentifier);
                        pipelineGraph.addVertex(newVertex);
                        pipelineGraph.removeEdge(executorEdge);
                        pipelineGraph.addEdge(selected, newVertex, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                        pipelineGraph.addEdge(newVertex, target, new MaskMethodEdge(MaskGraphVertex.SELF, MaskMethodVertex.EXECUTOR));
                        moveVertexToMouse(newVertex, mouseEvent);
                        pipelineGraph.refresh();
                        pipelineGraph.clearSelection();
                        pipelineGraph.addSelectionCell(pipelineGraph.getCellForVertex(newVertex));
                    }
                });
            } else {
                resultMenu.add(new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                    public void actionPerformed(ActionEvent e) {
                        MaskMethodVertex newVertex = new MaskMethodVertex(method, selectedResultClass);
                        String targetIdentifier = target.getIdentifier();
                        newVertex.setIdentifier(targetIdentifier == null ? String.valueOf(newVertex.hashCode()) : targetIdentifier);
                        pipelineGraph.addVertex(newVertex);
                        pipelineGraph.removeEdge(executorEdge);
                        pipelineGraph.addEdge(selected, newVertex, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                        pipelineGraph.addEdge(newVertex, target, new MaskMethodEdge(MaskGraphVertex.SELF, MaskMethodVertex.EXECUTOR));
                        moveVertexToMouse(newVertex, mouseEvent);
                        pipelineGraph.refresh();
                        pipelineGraph.clearSelection();
                        pipelineGraph.addSelectionCell(pipelineGraph.getCellForVertex(newVertex));
                    }
                });
            }
        });
        return resultMenu;
    }

    private JScrollMenu buildTransformationSubMenu(MouseEvent mouseEvent, MaskGraphVertex<?> selected, String resultName, Class<? extends Mask<?, ?>> selectedResultClass) {
        JScrollMenu resultMenu = new JScrollMenu(resultName);
        MaskReflectUtil.getMaskMethods(selectedResultClass).forEach(method -> {
            if (method.getDeclaringClass().equals(MapMaskMethods.class)) {
                resultMenu.add(new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                    public void actionPerformed(ActionEvent e) {
                        MapMaskMethodVertex newVertex = new MapMaskMethodVertex(method);
                        pipelineGraph.addVertex(newVertex);
                        pipelineGraph.addEdge(selected, newVertex, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                        moveVertexToMouse(newVertex, mouseEvent);
                        pipelineGraph.refresh();
                        pipelineGraph.clearSelection();
                        pipelineGraph.addSelectionCell(pipelineGraph.getCellForVertex(newVertex));
                    }
                });
            } else {
                resultMenu.add(new AbstractAction(MaskReflectUtil.getExecutableString(method)) {
                    public void actionPerformed(ActionEvent e) {
                        MaskMethodVertex newVertex = new MaskMethodVertex(method, selectedResultClass);
                        pipelineGraph.addVertex(newVertex);
                        pipelineGraph.addEdge(selected, newVertex, new MaskMethodEdge(resultName, MaskMethodVertex.EXECUTOR));
                        moveVertexToMouse(newVertex, mouseEvent);
                        pipelineGraph.refresh();
                        pipelineGraph.clearSelection();
                        pipelineGraph.addSelectionCell(pipelineGraph.getCellForVertex(newVertex));
                    }
                });
            }
        });
        return resultMenu;
    }

    private void moveVertexToMouse(MaskGraphVertex<?> newVertex, MouseEvent mouseEvent) {
        Geometry geometry = pipelineGraph.getCellForVertex(newVertex).getGeometry();
        geometry.setX(mouseEvent.getX());
        geometry.setY(mouseEvent.getY());
    }

    /**
     * @return Returns true if the given event should toggle selected cells.
     */
    public boolean isToggleEvent(MouseEvent event) {
        return event != null && (SwingUtilities.isLeftMouseButton(event) && event.isShiftDown()) || !SwingUtilities.isLeftMouseButton(event);
    }

    public boolean isPanningEvent(MouseEvent event) {
        return event != null && !event.isShiftDown() && getCellAt(event.getX(), event.getY()) == null;
    }

    public boolean shouldAutoScroll() {
        return autoScroll && !panningHandler.isActive();
    }
}
