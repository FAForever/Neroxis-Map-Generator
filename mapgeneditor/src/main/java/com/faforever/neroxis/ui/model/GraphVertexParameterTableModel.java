package com.faforever.neroxis.ui.model;

import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskGraphReflectUtil;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

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
    public Object getValueAt(int rowIndex, int columnIndex) {
        Parameter parameter = parameters.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> parameter.getName();
            case 1 -> vertex == null ? null : getParameterClass(parameter);
            case 2 -> vertex == null ? null : vertex.getParameterExpression(parameter);
            default -> null;
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
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> String.class;
            case 1 -> Class.class;
            default -> Object.class;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != 2) {
            return false;
        }

        Parameter parameter = parameters.get(rowIndex);

        return !Mask.class.isAssignableFrom(getParameterClass(parameter));
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 2 || vertex.getExecutable() == null || !isCellEditable(rowIndex, columnIndex)) {
            return;
        }

        Parameter parameter = parameters.get(rowIndex);

        if (MaskGraphReflectUtil.classIsNumeric(getParameterClass(parameter))
            && String.class.equals(aValue.getClass())
            && ((String) aValue).startsWith(".")) {
            aValue = "0" + aValue;
        }

        vertex.setParameter(parameter.getName(), aValue);
    }

    private Class<?> getParameterClass(Parameter parameter) {
        Class<?> parameterClass;
        if (Mask.class.isAssignableFrom(vertex.getExecutable().getDeclaringClass())) {
            parameterClass = MaskGraphReflectUtil.getActualTypeClass(vertex.getExecutorClass(),
                                                                     parameter.getParameterizedType());
        } else {
            parameterClass = parameter.getType();
        }
        return parameterClass;
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
                    if (vertex.getGraphAnnotationForParameter(parameter)
                              .map(annotation -> annotation.value().isBlank())
                              .orElse(true) && !(Mask.class.isAssignableFrom(getParameterClass(parameter)))) {
                        parameters.add(parameter);
                    }
                }
                fireTableRowsInserted(0, parameters.size());
            }
        }
    }
}
