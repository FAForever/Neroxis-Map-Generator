package com.faforever.neroxis.ui.model;

import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskReflectUtil;

import javax.swing.table.AbstractTableModel;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphVertexParameterTableModel extends AbstractTableModel {

    private final List<Parameter> parameters = new ArrayList<>();
    private MaskGraphVertex<?> vertex;

    @Override
    public int getRowCount() {
        return parameters.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> String.class;
            case 1 -> Class.class;
            default -> Object.class;
        };
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "Parameter Name";
            case 1 -> "Parameter Class";
            case 2 -> "Parameter Value";
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != 2) {
            return false;
        }

        Parameter parameter = parameters.get(rowIndex);

        return !Mask.class.isAssignableFrom(MaskReflectUtil.getActualTypeClass(vertex.getExecutorClass(), parameter.getParameterizedType()));
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 2 || vertex.getExecutable() == null || !isCellEditable(rowIndex, columnIndex)) {
            return;
        }

        Parameter parameter = parameters.get(rowIndex);

        if (MaskReflectUtil.classIsNumeric(MaskReflectUtil.getActualTypeClass(vertex.getExecutorClass(), parameter.getParameterizedType()))
                && String.class.equals(aValue.getClass())
                && ((String) aValue).startsWith(".")) {
            aValue = "0" + aValue;
        }

        vertex.setParameter(parameter.getName(), aValue);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Parameter parameter = parameters.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> parameter.getName();
            case 1 -> vertex == null ? null : MaskReflectUtil.getActualTypeClass(vertex.getExecutorClass(), parameter.getParameterizedType());
            case 2 -> vertex == null ? null : vertex.getParameterExpression(parameter);
            default -> null;
        };
    }

    public void setVertex(MaskGraphVertex<?> vertex) {
        this.vertex = vertex;
        updateTableModel();
    }

    public void updateTableModel() {
        fireTableRowsDeleted(0, parameters.size());
        parameters.clear();
        if (vertex != null) {
            Executable executable = vertex.getExecutable();
            if (executable != null) {
                for (Parameter parameter : executable.getParameters()) {
                    if (Arrays.stream(executable.getAnnotationsByType(GraphParameter.class)).noneMatch(annotation -> parameter.getName().equals(annotation.name())
                            && (!annotation.value().equals("")))) {
                        parameters.add(parameter);
                    }
                }
                fireTableRowsInserted(0, parameters.size());
            }
        }
    }
}
