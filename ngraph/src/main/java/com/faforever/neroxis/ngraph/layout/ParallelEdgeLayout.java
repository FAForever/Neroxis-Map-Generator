package com.faforever.neroxis.ngraph.layout;

import com.faforever.neroxis.ngraph.model.CellPath;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import com.faforever.neroxis.ngraph.view.GraphView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParallelEdgeLayout extends GraphLayout {
    /**
     * Specifies the spacing between the edges. Default is 20.
     */
    protected int spacing;

    /**
     * Constructs a new stack layout layout for the specified graph,
     * spacing, orientation and offset.
     */
    public ParallelEdgeLayout(Graph graph) {
        this(graph, 20);
    }

    /**
     * Constructs a new stack layout layout for the specified graph,
     * spacing, orientation and offset.
     */
    public ParallelEdgeLayout(Graph graph, int spacing) {
        super(graph);
        this.spacing = spacing;
    }

    @Override
    public void execute(ICell parent) {
        Map<String, List<ICell>> lookup = findParallels(parent);

        graph.getModel().beginUpdate();
        try {

            for (List<ICell> parallels : lookup.values()) {
                if (parallels.size() > 1) {
                    layout(parallels);
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }
    }

    protected Map<String, List<ICell>> findParallels(ICell parent) {
        Map<String, List<ICell>> lookup = new HashMap<>();
        IGraphModel model = graph.getModel();
        int childCount = model.getChildCount(parent);

        for (int i = 0; i < childCount; i++) {
            ICell child = model.getChildAt(parent, i);

            if (!isEdgeIgnored(child)) {
                String id = getEdgeId(child);

                if (id != null) {
                    if (!lookup.containsKey(id)) {
                        lookup.put(id, new ArrayList<>());
                    }

                    lookup.get(id).add(child);
                }
            }
        }

        return lookup;
    }

    protected String getEdgeId(ICell edge) {
        GraphView view = graph.getView();
        CellState state = view.getState(edge);
        ICell src = (state != null) ? state.getVisibleTerminal(true) : view.getVisibleTerminal(edge, true);
        ICell trg = (state != null) ? state.getVisibleTerminal(false) : view.getVisibleTerminal(edge, false);

        if (src != null && trg != null) {
            String srcId = CellPath.create(src);
            String trgId = CellPath.create(trg);

            return (srcId.compareTo(trgId) > 0) ? trgId + "-" + srcId : srcId + "-" + trgId;
        }

        return null;
    }

    protected void layout(List<ICell> parallels) {
        ICell edge = parallels.get(0);
        IGraphModel model = graph.getModel();
        Geometry src = model.getGeometry(model.getTerminal(edge, true));
        Geometry trg = model.getGeometry(model.getTerminal(edge, false));

        // Routes multiple loops
        if (src == trg) {
            double x0 = src.getX() + src.getWidth() + this.spacing;
            double y0 = src.getY() + src.getHeight() / 2;

            for (ICell parallel : parallels) {
                route(parallel, x0, y0);
                x0 += spacing;
            }
        } else if (src != null && trg != null) {
            // Routes parallel edges
            double scx = src.getX() + src.getWidth() / 2;
            double scy = src.getY() + src.getHeight() / 2;

            double tcx = trg.getX() + trg.getWidth() / 2;
            double tcy = trg.getY() + trg.getHeight() / 2;

            double dx = tcx - scx;
            double dy = tcy - scy;

            double len = Math.sqrt(dx * dx + dy * dy);

            double x0 = scx + dx / 2;
            double y0 = scy + dy / 2;

            double nx = dy * spacing / len;
            double ny = dx * spacing / len;

            x0 += nx * (parallels.size() - 1) / 2;
            y0 -= ny * (parallels.size() - 1) / 2;

            for (ICell parallel : parallels) {
                route(parallel, x0, y0);
                x0 -= nx;
                y0 += ny;
            }
        }
    }

    protected void route(ICell edge, double x, double y) {
        if (graph.isCellMovable(edge)) {
            setEdgePoints(edge, List.of(new PointDouble(x, y)));
        }
    }
}
