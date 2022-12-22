package com.faforever.neroxis.ui.components;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Supplier;

@Getter
public class CloseableTabComponent extends JPanel {
    private final JLabel title = new JLabel();
    private final JButton closeButton = new JButton();
    private final JTabbedPane tabbedPane;
    @Setter
    private Supplier<Boolean> shouldCloseSupplier = () -> true;

    public CloseableTabComponent(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
        setLayout(new FlowLayout());
        setBackground(new Color(0, 0, 0, 0));
        add(title);
        add(closeButton);
        closeButton.setBorder(BorderFactory.createRaisedSoftBevelBorder());
        closeButton.setText("x");
        closeButton.addActionListener(e -> close());
    }

    private void close() {
        if (shouldCloseSupplier.get()) {
            int index = tabbedPane.indexOfTabComponent(this);
            tabbedPane.setSelectedIndex(Math.max(-1, index - 1));
            tabbedPane.removeTabAt(index);
            Arrays.stream(closeButton.getActionListeners()).forEach(closeButton::removeActionListener);
        }
    }

    public void setTitle(String title) {
        int index = tabbedPane.indexOfTabComponent(this);
        this.title.setText(title);
        tabbedPane.setTitleAt(index, title);
    }
}
