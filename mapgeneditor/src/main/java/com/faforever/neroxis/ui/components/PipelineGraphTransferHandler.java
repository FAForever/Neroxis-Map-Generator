package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.handler.GraphTransferHandler;
import com.faforever.neroxis.ngraph.swing.util.GraphTransferable;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.Objects;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

public class PipelineGraphTransferHandler extends GraphTransferHandler {

    private final PipelineGraphComponent pipelineGraphComponent;

    public PipelineGraphTransferHandler(PipelineGraphComponent pipelineGraphComponent) {
        this.pipelineGraphComponent = pipelineGraphComponent;
    }

    @Override
    public GraphTransferable createGraphTransferable(GraphComponent graphComponent, List<ICell> cells,
                                                     RectangleDouble bounds, ImageIcon icon) {
        return new GraphTransferable(cells, bounds, icon);
    }

    @Override
    public void exportDone(JComponent c, Transferable data, int action) {
        if (c instanceof GraphComponent && data instanceof GraphTransferable) {
            pipelineGraphComponent.getGraph()
                                  .getAllEdges(((GraphTransferable) data).getCells())
                                  .stream()
                                  .map(ICell::getGeometry)
                                  .filter(Objects::nonNull)
                                  .map(Geometry::getPoints)
                                  .filter(Objects::nonNull)
                                  .forEach(List::clear);
        }
        super.exportDone(c, data, action);
    }
}
