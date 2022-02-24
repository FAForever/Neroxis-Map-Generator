package com.faforever.neroxis.ui.renderer;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class StringListCellRenderer<T> extends DefaultListCellRenderer {
    private final Function<T, String> stringConverter;

    public StringListCellRenderer(Function<T, String> stringConverter) {
        this.stringConverter = stringConverter;
        setOpaque(true);
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(value == null ? "" : stringConverter.apply((T) value));
        return this;
    }
}
