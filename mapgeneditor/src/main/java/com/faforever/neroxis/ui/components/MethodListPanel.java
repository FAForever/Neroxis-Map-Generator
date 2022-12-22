package com.faforever.neroxis.ui.components;

import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.renderer.StringListCellRenderer;
import com.faforever.neroxis.ui.transfer.GraphMethodListTransferHandler;
import com.faforever.neroxis.util.MaskGraphReflectUtil;
import com.github.therapi.runtimejavadoc.MethodJavadoc;
import com.github.therapi.runtimejavadoc.ParamJavadoc;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.faforever.neroxis.graph.domain.MaskMethodVertex.EXECUTOR;

public class MethodListPanel extends JPanel {
    private final JComboBox<Class<? extends Mask<?, ?>>> classComboBox = new JComboBox<>();
    private final JTextField methodSearchField = new JTextField();
    private final DefaultListModel<Executable> methodListModel = new DefaultListModel<>();
    private final JList<Executable> methodList = new JList<>(methodListModel);
    private final JTextPane methodDescription = new JTextPane();

    public MethodListPanel() {
        setLayout(new GridBagLayout());
        setupMethodSelection();
    }

    private void setupMethodSelection() {
        GridBagConstraints constraintsCombo = new GridBagConstraints();
        constraintsCombo.fill = GridBagConstraints.BOTH;
        constraintsCombo.gridx = 0;
        constraintsCombo.weightx = 1;
        constraintsCombo.gridy = 0;
        constraintsCombo.weighty = 0;
        add(classComboBox, constraintsCombo);

        MaskGraphReflectUtil.getConcreteMaskClasses().forEach(classComboBox::addItem);
        classComboBox.setRenderer(new StringListCellRenderer<Class<?>>(Class::getSimpleName));
        classComboBox.addActionListener(e -> updateMethodList());

        GridBagConstraints constraintsSearch = new GridBagConstraints();
        constraintsSearch.fill = GridBagConstraints.BOTH;
        constraintsSearch.gridx = 0;
        constraintsSearch.weightx = 1;
        constraintsSearch.gridy = 1;
        constraintsSearch.weighty = 0;
        add(methodSearchField, constraintsSearch);
        methodSearchField.addCaretListener(e -> updateMethodList());
        updateMethodList();

        GridBagConstraints constraintsList = new GridBagConstraints();
        constraintsList.fill = GridBagConstraints.BOTH;
        constraintsList.gridx = 0;
        constraintsList.weightx = 1;
        constraintsList.gridy = 2;
        constraintsList.weighty = 1;

        JScrollPane jScrollPane = new JScrollPane(methodList);
        add(jScrollPane, constraintsList);
        methodList.setCellRenderer(new StringListCellRenderer<>(this::getExecutableFormat));
        methodList.addListSelectionListener(e -> updateMethodDescription());
        methodList.setDragEnabled(true);
        methodList.setTransferHandler(new GraphMethodListTransferHandler(classComboBox));

        GridBagConstraints constraintsTextPane = new GridBagConstraints();
        constraintsTextPane.fill = GridBagConstraints.BOTH;
        constraintsTextPane.gridx = 0;
        constraintsTextPane.weightx = 1;
        constraintsTextPane.gridy = 3;
        constraintsTextPane.weighty = 1;

        methodDescription.setContentType("text/html");
        JScrollPane scrollPane = new JScrollPane(methodDescription);
        add(scrollPane, constraintsTextPane);
    }

    private String getExecutableFormat(Executable method) {
        return String.format("%s (%s)", method.getName(), Arrays.stream(method.getParameters())
                                                                .map(Parameter::getName)
                                                                .collect(Collectors.joining(", ")));
    }

    private void updateMethodList() {
        Class<? extends Mask<?, ?>> selectedClass = (Class<? extends Mask<?, ?>>) classComboBox.getSelectedItem();

        List<Method> methods = new ArrayList<>(MaskGraphReflectUtil.getMaskGraphMethods(selectedClass));

        Arrays.stream(MapMaskMethods.class.getDeclaredMethods())
              .filter(method -> Arrays.stream(method.getParameters())
                                      .anyMatch(param -> param.getName().equals(EXECUTOR)
                                                         && param.getType() == selectedClass))
              .forEach(methods::add);

        String methodSearchText = methodSearchField.getText();
        methods.removeIf(method -> (!methodSearchText.isBlank() && !method.getName().contains(methodSearchText)));

        methodListModel.clear();
        methodListModel.addAll(methods);
    }

    private void updateMethodDescription() {
        methodDescription.setText(getFormattedMethodJavadoc(methodList.getSelectedValue()));
    }

    private String getFormattedMethodJavadoc(Executable executable) {
        if (executable == null) {
            return null;
        }
        MethodJavadoc methodJavadoc = MaskGraphReflectUtil.getJavadoc(executable);
        StringBuilder paramString = new StringBuilder();
        Class<? extends Mask<?, ?>> clazz = (Class<? extends Mask<?, ?>>) classComboBox.getSelectedItem();
        Parameter[] parameters = executable.getParameters();
        for (int i = 0; i < methodJavadoc.getParams().size(); ++i) {
            ParamJavadoc param = methodJavadoc.getParams().get(i);
            String paramType = MaskGraphReflectUtil.getActualTypeClass(clazz, parameters[i].getParameterizedType())
                                                   .getSimpleName();
            paramString.append(String.format("<b>%s</b>: <i>%s</i> - %s<br>", param.getName(), paramType,
                                             param.getComment().toString()));
        }

        return ("<h2>"
                + getExecutableFormat(executable)
                + "</h2>"
                + methodJavadoc.getComment().toString()
                + "<br><br>"
                + paramString).replaceAll("\\{@link\\s(\\w*)\\s(\\w*)}", "<i>$1</i>");
    }

    public void setVertex(MaskGraphVertex<?> vertex) {
        if (vertex == null) {
            return;
        }
        Class<? extends Mask<?, ?>> executorClass = vertex.getExecutorClass();
        classComboBox.setSelectedItem(executorClass);
        methodList.setSelectedValue(vertex.getExecutable(), true);
    }
}
