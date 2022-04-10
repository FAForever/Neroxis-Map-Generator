package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.ui.listener.LostFocusListener;
import com.faforever.neroxis.ui.model.GraphVertexParameterTableModel;
import com.faforever.neroxis.ui.renderer.StringTableCellRenderer;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import static javax.swing.SwingConstants.CENTER;

public class MaskGraphVertexEditPanel extends JPanel {
    private final GraphVertexParameterTableModel parameterTableModel = new GraphVertexParameterTableModel();
    private final JTable parametersTable = new JTable(parameterTableModel);
    private final JTextField identifierTextField = new JTextField();
    private MaskGraphVertex<?> vertex;
    private final PipelinePanel pipelinePanel;
    private PipelineGraph graph;

    public MaskGraphVertexEditPanel(PipelinePanel pipelinePanel) {
        this.pipelinePanel = pipelinePanel;
        setLayout(new GridBagLayout());
        setupIdentifierTextField();
        setupParametersTable();
    }

    public void setGraphComponent(PipelineGraphComponent graphComponent) {
        graph = graphComponent.getGraph();
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
        if (vertex != null) {
            vertex.setIdentifier(identifierTextField.getText());
            pipelinePanel.updateIdentifiers(vertex);
        }
    }

    private void setupParametersTable() {
        parametersTable.setDefaultRenderer(Class.class, new StringTableCellRenderer<Class<?>>(Class::getSimpleName));
        parametersTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        parametersTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        parametersTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        parametersTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        parametersTable.setPreferredSize(new Dimension(300, -1));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridwidth = 2;
        constraints.gridy = 1;
        constraints.weighty = 1;

        add(parametersTable, constraints);
    }

    public void setVertex(MaskGraphVertex<?> vertex) {
        this.vertex = vertex;
        updatePanel();
    }

    public void updatePanel() {
        if (vertex != null && !graph.containsVertex(vertex)) {
            setVertex(null);
            return;
        }

        parameterTableModel.setVertex(vertex);
        identifierTextField.setText(vertex == null ? null : vertex.getIdentifier());
        parametersTable.doLayout();
    }

}
