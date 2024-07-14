package com.faforever.neroxis.visualization;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.vector.Vector2;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class EntryPanel extends JPanel {
    private final JLabel titleLabel = new JLabel();
    private final JLabel valueLabel = new JLabel();
    private final MaskPanel maskPanel = new MaskPanel(this);

    public EntryPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        setupImagePanel();
        setupLabels();
    }

    private void setupImagePanel() {
        add(maskPanel, BorderLayout.CENTER);
    }

    private void setupLabels() {
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(titleLabel, BorderLayout.NORTH);

        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setMinimumSize(new Dimension(100, 25));
        valueLabel.setPreferredSize(new Dimension(100, 25));
        valueLabel.setMaximumSize(new Dimension(100, 25));

        add(valueLabel, BorderLayout.SOUTH);
    }

    public void setMask(Mask<?, ?> mask) {
        maskPanel.setMask(mask);
        titleLabel.setText(String.format("Name: %s Size: %d", mask.getName(), mask.getSize()));
        repaint();
        setValueLabel();
    }

    public void setValueLabel() {
        if (maskPanel.getMask() != null) {
            Vector2 maskCoords = maskPanel.getMouseOnMask();
            if (maskPanel.getMask().inBounds(maskCoords)) {
                valueLabel.setText(String.format("X: %5.0f, Y: %5.0f Value: %s", maskCoords.getX(), maskCoords.getY(),
                        maskPanel.getMask().get(maskCoords).toString()));
            }
        }
    }

}
