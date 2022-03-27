/**
 * Copyright (c) 2008, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.swing.util;

import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.view.Graph;
import java.awt.event.ActionEvent;
import java.io.Serial;
import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 *
 */
public class GraphActions {
    static final Action deleteAction = new DeleteAction("delete");
    static final Action editAction = new EditAction("edit");
    static final Action groupAction = new GroupAction("group");
    static final Action ungroupAction = new UngroupAction("ungroup");
    static final Action removeFromParentAction = new RemoveFromParentAction("removeFromParent");
    static final Action updateGroupBoundsAction = new UpdateGroupBoundsAction("updateGroupBounds");
    static final Action selectAllAction = new SelectAction("selectAll");
    static final Action selectVerticesAction = new SelectAction("vertices");
    static final Action selectEdgesAction = new SelectAction("edges");
    static final Action selectNoneAction = new SelectAction("selectNone");
    static final Action selectNextAction = new SelectAction("selectNext");
    static final Action selectPreviousAction = new SelectAction("selectPrevious");
    static final Action selectParentAction = new SelectAction("selectParent");
    static final Action selectChildAction = new SelectAction("selectChild");
    static final Action collapseAction = new FoldAction("collapse");
    static final Action expandAction = new FoldAction("expand");
    static final Action enterGroupAction = new DrillAction("enterGroup");
    static final Action exitGroupAction = new DrillAction("exitGroup");
    static final Action homeAction = new DrillAction("home");
    static final Action zoomActualAction = new ZoomAction("actual");
    static final Action zoomInAction = new ZoomAction("zoomIn");
    static final Action zoomOutAction = new ZoomAction("zoomOut");
    static final Action toBackAction = new LayerAction("toBack");
    static final Action toFrontAction = new LayerAction("toFront");

    /**
     * @return the delete action
     */
    public static Action getDeleteAction() {
        return deleteAction;
    }

    /**
     * @return the edit action
     */
    public static Action getEditAction() {
        return editAction;
    }

    /**
     * @return the edit action
     */
    public static Action getGroupAction() {
        return groupAction;
    }

    /**
     * @return the edit action
     */
    public static Action getUngroupAction() {
        return ungroupAction;
    }

    /**
     * @return the edit action
     */
    public static Action getRemoveFromParentAction() {
        return removeFromParentAction;
    }

    /**
     * @return the edit action
     */
    public static Action getUpdateGroupBoundsAction() {
        return updateGroupBoundsAction;
    }

    /**
     * @return the select all action
     */
    public static Action getSelectAllAction() {
        return selectAllAction;
    }

    /**
     * @return the select vertices action
     */
    public static Action getSelectVerticesAction() {
        return selectVerticesAction;
    }

    /**
     * @return the select edges action
     */
    public static Action getSelectEdgesAction() {
        return selectEdgesAction;
    }

    /**
     * @return the select none action
     */
    public static Action getSelectNoneAction() {
        return selectNoneAction;
    }

    /**
     * @return the select next action
     */
    public static Action getSelectNextAction() {
        return selectNextAction;
    }

    /**
     * @return the select previous action
     */
    public static Action getSelectPreviousAction() {
        return selectPreviousAction;
    }

    /**
     * @return the select parent action
     */
    public static Action getSelectParentAction() {
        return selectParentAction;
    }

    /**
     * @return the select child action
     */
    public static Action getSelectChildAction() {
        return selectChildAction;
    }

    /**
     * @return the go into action
     */
    public static Action getEnterGroupAction() {
        return enterGroupAction;
    }

    /**
     * @return the go up action
     */
    public static Action getExitGroupAction() {
        return exitGroupAction;
    }

    /**
     * @return the home action
     */
    public static Action getHomeAction() {
        return homeAction;
    }

    /**
     * @return the collapse action
     */
    public static Action getCollapseAction() {
        return collapseAction;
    }

    /**
     * @return the expand action
     */
    public static Action getExpandAction() {
        return expandAction;
    }

    /**
     * @return the zoom actual action
     */
    public static Action getZoomActualAction() {
        return zoomActualAction;
    }

    /**
     * @return the zoom in action
     */
    public static Action getZoomInAction() {
        return zoomInAction;
    }

    /**
     * @return the zoom out action
     */
    public static Action getZoomOutAction() {
        return zoomOutAction;
    }

    /**
     * @return the action that moves cell(s) to the backmost layer
     */
    public static Action getToBackAction() {
        return toBackAction;
    }

    /**
     * @return the action that moves cell(s) to the frontmost layer
     */
    public static Action getToFrontAction() {
        return toFrontAction;
    }

    /**
     * @return Returns the graph for the given action event.
     */
    public static Graph getGraph(ActionEvent e) {
        Object source = e.getSource();

        if (source instanceof GraphComponent) {
            return ((GraphComponent) source).getGraph();
        }

        return null;
    }

    public static class EditAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 4610112721356742702L;


        public EditAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof GraphComponent) {
                ((GraphComponent) e.getSource()).startEditing();
            }
        }

    }

    public static class DeleteAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -8212339796803275529L;


        public DeleteAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                graph.removeCells();
            }
        }

    }

    public static class GroupAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -4718086600089409092L;


        public GroupAction(String name) {
            super(name);
        }

        protected int getGroupBorder(Graph graph) {
            return 2 * graph.getGridSize();

        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                graph.setSelectionCell(graph.groupCells(null, getGroupBorder(graph)));
            }
        }

    }

    public static class UngroupAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 2247770767961318251L;


        public UngroupAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                graph.setSelectionCells(graph.ungroupCells());
            }
        }

    }

    public static class RemoveFromParentAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 7169443038859140811L;


        public RemoveFromParentAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                graph.removeCellsFromParent();
            }
        }

    }

    public static class UpdateGroupBoundsAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -4718086600089409092L;


        public UpdateGroupBoundsAction(String name) {
            super(name);
        }

        protected int getGroupBorder(Graph graph) {
            return 2 * graph.getGridSize();
        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                graph.updateGroupBounds(null, getGroupBorder(graph));
            }
        }

    }

    public static class LayerAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 562519299806253741L;


        public LayerAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                boolean toBack = getValue(Action.NAME).toString().equalsIgnoreCase("toBack");
                graph.orderCells(toBack);
            }
        }

    }

    public static class FoldAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 4078517503905239901L;


        public FoldAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                boolean collapse = getValue(Action.NAME).toString().equalsIgnoreCase("collapse");
                graph.foldCells(collapse);
            }
        }

    }

    public static class DrillAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 5464382323663870291L;


        public DrillAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                String name = getValue(Action.NAME).toString();

                if (name.equalsIgnoreCase("enterGroup")) {
                    graph.enterGroup();
                } else if (name.equalsIgnoreCase("exitGroup")) {
                    graph.exitGroup();
                } else {
                    graph.home();
                }
            }
        }

    }

    public static class ZoomAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -7500195051313272384L;


        public ZoomAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();

            if (source instanceof GraphComponent) {
                String name = getValue(Action.NAME).toString();
                GraphComponent graphComponent = (GraphComponent) source;

                if (name.equalsIgnoreCase("zoomIn")) {
                    graphComponent.zoomIn();
                } else if (name.equalsIgnoreCase("zoomOut")) {
                    graphComponent.zoomOut();
                } else {
                    graphComponent.zoomActual();
                }
            }
        }

    }

    public static class SelectAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = 6501585024845668187L;

        public SelectAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            Graph graph = getGraph(e);

            if (graph != null) {
                String name = getValue(Action.NAME).toString();

                if (name.equalsIgnoreCase("selectAll")) {
                    graph.selectAll();
                } else if (name.equalsIgnoreCase("selectNone")) {
                    graph.clearSelection();
                } else if (name.equalsIgnoreCase("selectNext")) {
                    graph.selectNextCell();
                } else if (name.equalsIgnoreCase("selectPrevious")) {
                    graph.selectPreviousCell();
                } else if (name.equalsIgnoreCase("selectParent")) {
                    graph.selectParentCell();
                } else if (name.equalsIgnoreCase("vertices")) {
                    graph.selectVertices();
                } else if (name.equalsIgnoreCase("edges")) {
                    graph.selectEdges();
                } else {
                    graph.selectChildCell();
                }
            }
        }

    }

}
