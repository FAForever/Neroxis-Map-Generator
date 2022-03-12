package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.ui.listener.LostFocusListener;
import com.faforever.neroxis.ui.model.GraphVertexParameterTableModel;
import com.faforever.neroxis.ui.renderer.StringTableCellRenderer;
import lombok.Setter;
import org.jgrapht.Graph;
import org.jungrapht.visualization.VisualizationViewer;

import javax.swing.*;
import java.awt.*;

import static javax.swing.SwingConstants.CENTER;

public class MaskGraphVertexEditPanel extends JPanel {
    private final GraphVertexParameterTableModel parameterTableModel = new GraphVertexParameterTableModel();
    private final JTable parametersTable = new JTable(parameterTableModel);
    private final JTextField identifierTextField = new JTextField();
    private MaskGraphVertex<?> vertex;

    @Setter
    private VisualizationViewer<MaskGraphVertex<?>, MaskMethodEdge> visualizationViewer;

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
        Graph<MaskGraphVertex<?>, MaskMethodEdge> graph = visualizationViewer.getVisualizationModel().getGraph();

        MaskGraphVertex<?> nextVertex = vertex;
        while (nextVertex != null) {
            nextVertex.setIdentifier(identifierTextField.getText());
            nextVertex = graph.outgoingEdgesOf(nextVertex).stream().filter(edge ->
                            MaskGraphVertex.SELF.equals(edge.getResultName()) && MaskMethodVertex.EXECUTOR.equals(edge.getParameterName())
                    ).map(graph::getEdgeTarget)
                    .findFirst()
                    .orElse(null);
        }

        MaskGraphVertex<?> previousVertex = vertex;
        while (previousVertex != null) {
            previousVertex.setIdentifier(identifierTextField.getText());
            previousVertex = graph.incomingEdgesOf(previousVertex).stream().filter(edge ->
                            MaskGraphVertex.SELF.equals(edge.getResultName()) && MaskMethodVertex.EXECUTOR.equals(edge.getParameterName())
                    ).map(graph::getEdgeSource)
                    .findFirst()
                    .orElse(null);
        }

        visualizationViewer.repaint();
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
        constraints.gridy = 4;
        constraints.weighty = 1;

        add(parametersTable, constraints);
    }

    public void setVertex(MaskGraphVertex<?> vertex) {
        this.vertex = vertex;
        updatePanel();
    }

    public void updatePanel() {
        Graph<MaskGraphVertex<?>, MaskMethodEdge> graph = visualizationViewer.getVisualizationModel().getGraph();
        if (vertex != null && !graph.containsVertex(vertex)) {
            setVertex(null);
            return;
        }

        parameterTableModel.setVertex(vertex);
        identifierTextField.setText(vertex == null ? null : vertex.getIdentifier());
    }

}
