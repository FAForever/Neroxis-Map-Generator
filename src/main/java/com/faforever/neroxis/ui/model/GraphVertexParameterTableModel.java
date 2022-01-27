package com.faforever.neroxis.ui.model;

import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskReflectUtil;

import javax.swing.table.AbstractTableModel;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphVertexParameterTableModel extends AbstractTableModel {

    private final List<Parameter> items = new ArrayList<>();
    private MaskGraphVertex vertex;

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return Class.class;
            case 2:
                return Object.class;
        }
        return Object.class;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Parameter Name";
            case 1:
                return "Parameter Class";
            case 2:
                return "Parameter Value";
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != 2) {
            return false;
        }
        Parameter item = items.get(rowIndex);
        return !Mask.class.isAssignableFrom(MaskReflectUtil.getActualParameterClass(vertex.getMaskClass(), item));
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 2) {
            return;
        }

        vertex.setParameter(items.get(rowIndex).getName(), Boolean.valueOf((String) aValue));
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Parameter item = items.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return item.getName();
            case 1:
                return vertex == null ? null : MaskReflectUtil.getActualParameterClass(vertex.getMaskClass(), item);
            case 2:
                return vertex == null ? null : vertex.getParameter(item.getName());
        }
        return null;
    }

    public void setParameters(Parameter... parameters) {
        int previousNumRows = items.size();
        items.clear();
        fireTableRowsDeleted(0, previousNumRows);
        items.addAll(Arrays.asList(parameters));
        fireTableRowsInserted(0, items.size());
    }

    public void setVertex(MaskGraphVertex vertex) {
        this.vertex = vertex;
        setParameters();
    }
}
