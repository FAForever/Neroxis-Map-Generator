package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.handler.ConnectionHandler;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JOptionPane;

public class PipelineConnectionHandler extends ConnectionHandler {
    private final PipelineGraphComponent pipelineGraphComponent;

    public PipelineConnectionHandler(PipelineGraphComponent graphComponent) {
        super(graphComponent);
        pipelineGraphComponent = graphComponent;
    }

    @Override
    public String validateConnection(ICell source, ICell target) {
        if (source == null
            || target == null
            || source.getParent() == target.getParent()
            || !source.isVertex()
            || !target.isVertex()
            || target.getValue() == null
            || source.getValue() == null) {
            return "";
        }
        PipelineGraph graph = pipelineGraphComponent.getGraph();
        MaskGraphVertex<?> sourceVertex = graph.getVertexForCell(source);
        MaskGraphVertex<?> targetVertex = graph.getVertexForCell(target);
        Class<? extends Mask<?, ?>> maskParameterClass = targetVertex.getMaskParameterClass((String) target.getValue());
        if (maskParameterClass == null || !maskParameterClass.isAssignableFrom(
                sourceVertex.getResultClass((String) source.getValue()))) {
            return "";
        }
        if (MaskMethodVertex.EXECUTOR.equals(target.getValue()) && graph.outgoingEdgesOf(sourceVertex)
                                                                        .stream()
                                                                        .anyMatch(
                                                                                edge -> MaskMethodVertex.EXECUTOR.equals(
                                                                                        edge.getParameterName())
                                                                                        && edge.getResultName()
                                                                                               .equals(source.getValue()))) {
            return "";
        }
        return super.validateConnection(source, target);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isActive()) {
            if (error != null) {
                if (error.length() > 0) {
                    JOptionPane.showMessageDialog(graphComponent, error);
                }
            } else if (first != null) {
                PipelineGraph graph = pipelineGraphComponent.getGraph();
                if (connectPreview.isActive() && (marker.hasValidState()
                                                  || isCreateTarget()
                                                  || graph.isAllowDanglingEdges())) {
                    ICell cell = connectPreview.stop(true, e);
                    MaskGraphVertex<?> source = graph.getVertexForCell(cell.getSource());
                    MaskGraphVertex<?> target = graph.getVertexForCell(cell.getTarget());
                    graph.removeCells(List.of(cell));
                    graph.addEdge(source, target, new MaskMethodEdge((String) cell.getSource().getValue(),
                                                                     (String) cell.getTarget().getValue()));
                    e.consume();
                }
            }
        }
        reset();
    }
}
