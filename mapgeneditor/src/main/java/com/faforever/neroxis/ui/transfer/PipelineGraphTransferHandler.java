package com.faforever.neroxis.ui.transfer;

import com.faforever.neroxis.graph.domain.MapMaskMethodVertex;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.swing.handler.GraphTransferHandler;
import com.faforever.neroxis.ngraph.swing.util.GraphTransferable;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ui.components.PipelineGraphComponent;
import com.faforever.neroxis.util.MaskGraphReflectUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PipelineGraphTransferHandler extends GraphTransferHandler {
    private final PipelineGraphComponent pipelineGraphComponent;

    public PipelineGraphTransferHandler(PipelineGraphComponent pipelineGraphComponent) {
        this.pipelineGraphComponent = pipelineGraphComponent;
    }

    @Override
    public boolean importData(JComponent c, Transferable t) {
        boolean result = false;

        if (isLocalDrag()) {
            // Enables visual feedback on the Mac
            result = true;
        } else {
            try {
                updateImportCount(t);

                if (c instanceof GraphComponent) {
                    GraphComponent graphComponent = (GraphComponent) c;

                    if (graphComponent.isEnabled() && t.isDataFlavorSupported(GraphTransferable.dataFlavor)) {
                        GraphTransferable gt = (GraphTransferable) t.getTransferData(GraphTransferable.dataFlavor);

                        if (gt.getCells() != null) {
                            result = importGraphTransferable(graphComponent, gt);
                        }
                    } else if (graphComponent.isEnabled() && t.isDataFlavorSupported(
                            GraphMethodTransferable.dataFlavor)) {
                        GraphMethodTransferable methodTransferable = (GraphMethodTransferable) t.getTransferData(
                                GraphMethodTransferable.dataFlavor);
                        Class<? extends Mask<?, ?>> clazz = (Class<? extends Mask<?, ?>>) MaskGraphReflectUtil.getClassFromString(
                                methodTransferable.getExecutingClassName());
                        Class<?> methodClazz = MaskGraphReflectUtil.getClassFromString(
                                methodTransferable.getMethodClassName());
                        int numParams = methodTransferable.getParamClassNames().size();
                        Class<?>[] paramTypes = new Class[numParams];
                        for (int i = 0; i < numParams; i++) {
                            paramTypes[i] = MaskGraphReflectUtil.getClassFromString(
                                    methodTransferable.getParamClassNames().get(i));
                        }
                        Method method = methodClazz.getMethod(methodTransferable.getMethodName(), paramTypes);

                        MaskGraphVertex<?> vertex;
                        if (Mask.class.isAssignableFrom(methodClazz)) {
                            vertex = new MaskMethodVertex(method, clazz);
                        } else if (MapMaskMethods.class.isAssignableFrom(methodClazz)) {
                            vertex = new MapMaskMethodVertex(method);
                        } else {
                            throw new UnsupportedOperationException("Unknown class %s".formatted(clazz.getSimpleName()));
                        }

                        pipelineGraphComponent.getGraph().addVertex(vertex);
                        Point mousePosition = SwingUtilities.convertPoint(c, c.getMousePosition(), graphComponent.getGraphControl());
                        pipelineGraphComponent.moveVertexToMousePosition(vertex, mousePosition);
                        pipelineGraphComponent.getGraph().selectVertexIfExists(vertex);
                    }
                }
            } catch (Exception ex) {
                System.out.println("Failed to import data");
                ex.printStackTrace();
            }
        }

        return result;
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] flavors) {
        if (Arrays.stream(flavors).anyMatch(GraphMethodTransferable.dataFlavor::equals)) {
            return true;
        }

        return super.canImport(comp, flavors);
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

    @Override
    public GraphTransferable createGraphTransferable(GraphComponent graphComponent, List<ICell> cells,
                                                     RectangleDouble bounds, ImageIcon icon) {
        return new GraphTransferable(cells, bounds, icon);
    }
}
