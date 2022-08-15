package com.faforever.neroxis.ui.renderer;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class StringTableCellRenderer<T> extends DefaultTableCellRenderer {
    private final Function<T, String> stringConverter;

    public StringTableCellRenderer(Function<T, String> stringConverter) {
        this.stringConverter = stringConverter;
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setText(value == null ? "" : stringConverter.apply((T) value));
        return this;
    }
}
