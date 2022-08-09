package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.ui.listener.LostFocusListener;
import com.faforever.neroxis.ui.model.GraphVertexParameterTableModel;
import com.faforever.neroxis.ui.renderer.StringTableCellRenderer;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

import static javax.swing.SwingConstants.CENTER;

public class MaskGraphVertexEditPanel extends JPanel {
    private final GraphVertexParameterTableModel parameterTableModel = new GraphVertexParameterTableModel();
    private final JTable parametersTable = new JTable(parameterTableModel);
    private final JTextField identifierTextField = new JTextField();
    private MaskGraphVertex<?> vertex;
    @Setter
    private PipelinePane pipelinePane;

    public MaskGraphVertexEditPanel() {
        setLayout(new GridBagLayout());
        setupIdentifierTextField();
        setupParametersTable();
    }

    private void setupIdentifierTextField() {
        identifierTextField.addActionListener(e -> updateIdentifiers());
        identifierTextField.addFocusListener(new LostFocusListener(this::updateIdentifiers));
        GridBagConstraints textFieldConstraints = new GridBagConstraints();
        textFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        textFieldConstraints.gridx = 1;
        textFieldConstraints.weightx = 1;
        textFieldConstraints.gridy = 0;
        textFieldConstraints.weighty = 0;

        identifierTextField.setPreferredSize(new Dimension(200, 25));
        add(identifierTextField, textFieldConstraints);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.gridx = 0;
        labelConstraints.weightx = 1;
        labelConstraints.gridy = 0;
        labelConstraints.weighty = 0;

        JLabel identifierLabel = new JLabel("Identifier");
        identifierLabel.setPreferredSize(new Dimension(100, 25));
        identifierLabel.setHorizontalAlignment(CENTER);
        add(identifierLabel, labelConstraints);
    }

    private void updateIdentifiers() {
        if (vertex != null && pipelinePane != null) {
            String newIdentifier = identifierTextField.getText();
            pipelinePane.updateIdentifiers(vertex, newIdentifier);
        }
    }

    private void setupParametersTable() {
        parametersTable.setDefaultRenderer(Class.class, new StringTableCellRenderer<Class<?>>(Class::getSimpleName));
        parametersTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridwidth = 2;
        constraints.gridy = 1;
        constraints.weighty = 1;
        JScrollPane parametersPane = new JScrollPane(parametersTable);
        parametersPane.setPreferredSize(new Dimension(0, 150));
        add(parametersPane, constraints);
        parameterTableModel.addTableModelListener(e -> updateVertexDefined(vertex));
        parametersTable.addPropertyChangeListener(evt -> updateVertexDefined(vertex));
    }

    private void updateVertexDefined(MaskGraphVertex<?> vertex) {
        if (vertex == null) {
            return;
        }

        ParameterConstraints parameterConstraints = ParameterConstraints.builder().build();
        GeneratorParameters generatorParameters = GeneratorParameters.builder()
                .terrainSymmetry(Symmetry.POINT2)
                .mapSize(512)
                .numTeams(2)
                .spawnCount(2)
                .biome(Biomes.loadBiome(Biomes.BIOMES_LIST.get(0)))
                .build();
        GeneratorGraphContext graphContext = new GeneratorGraphContext(0L, generatorParameters, parameterConstraints);
        pipelinePane.getGraph().setVertexDefined(vertex, vertex.isDefined(graphContext));
    }

    public void setVertex(MaskGraphVertex<?> vertex) {
        this.vertex = vertex;
        updatePanel();
    }

    public void updatePanel() {
        TableCellEditor cellEditor = parametersTable.getCellEditor();
        if (cellEditor != null) {
            cellEditor.stopCellEditing();
        }
        parameterTableModel.setVertex(vertex);
        identifierTextField.setText(vertex == null ? null : vertex.getIdentifier());
        parametersTable.doLayout();
        revalidate();
        repaint();
    }
}
