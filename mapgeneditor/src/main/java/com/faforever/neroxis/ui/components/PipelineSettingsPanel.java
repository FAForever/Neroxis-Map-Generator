package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.map.Symmetry;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PipelineSettingsPanel extends JPanel {
    private final JTextField seedTextField = new JTextField();
    private final JComboBox<Integer> mapSizeComboBox = new JComboBox<>();
    private final JComboBox<Integer> spawnCountComboBox = new JComboBox<>();
    private final JComboBox<Integer> numTeamsComboBox = new JComboBox<>();
    private final JComboBox<Symmetry> terrainSymmetryComboBox = new JComboBox<>();

    public PipelineSettingsPanel() {
        setLayout(new GridLayout(0, 2));
        setupMapOptions();
    }

    private void setupMapOptions() {
        add(new JLabel("Seed"));
        add(seedTextField);

        add(new JLabel("Map Size"));
        add(mapSizeComboBox);

        IntStream.range(4, 17).forEach(i -> mapSizeComboBox.addItem(i * 64));
        add(new JLabel("Spawn Count"));

        add(spawnCountComboBox);
        IntStream.range(2, 17).forEach(spawnCountComboBox::addItem);

        spawnCountComboBox.addActionListener(e -> {
            Object selected = numTeamsComboBox.getSelectedItem();
            numTeamsComboBox.removeAllItems();
            IntStream.range(1, 9)
                     .filter(i -> ((int) spawnCountComboBox.getSelectedItem() % i) == 0)
                     .forEach(numTeamsComboBox::addItem);
            if (selected != null) {
                numTeamsComboBox.setSelectedItem(selected);
            }
        });

        add(new JLabel("Num Teams"));
        add(numTeamsComboBox);

        numTeamsComboBox.addActionListener(e -> {
            Object selected = terrainSymmetryComboBox.getSelectedItem();
            terrainSymmetryComboBox.removeAllItems();
            if (numTeamsComboBox.getSelectedItem() != null) {
                Arrays.stream(Symmetry.values())
                      .filter(symmetry -> (symmetry.getNumSymPoints() % (int) numTeamsComboBox.getSelectedItem()) == 0)
                      .forEach(terrainSymmetryComboBox::addItem);
                if (selected != null) {
                    terrainSymmetryComboBox.setSelectedItem(selected);
                }
            }
        });

        spawnCountComboBox.setSelectedIndex(0);

        add(new JLabel("Terrain Symmetry"));
        add(terrainSymmetryComboBox);
    }

    public long getSeed() {
        String seedText = seedTextField.getText();
        if (seedText == null || seedText.isBlank()) {
            return new Random().nextLong();
        }

        try {
            return Long.parseLong(seedText);
        } catch (NumberFormatException e) {
            return seedText.hashCode();
        }
    }

    public int getNumTeams() {
        return (int) numTeamsComboBox.getSelectedItem();
    }

    public int getMapSize() {
        return (int) mapSizeComboBox.getSelectedItem();
    }

    public int getSpawnCount() {
        return (int) spawnCountComboBox.getSelectedItem();
    }

    public Symmetry getSymmetry() {
        return (Symmetry) terrainSymmetryComboBox.getSelectedItem();
    }
}
