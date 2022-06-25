package com.faforever.neroxis.ui.transfer;

import com.faforever.neroxis.mask.Mask;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GraphMethodListTransferHandler extends TransferHandler {
    private JComboBox<Class<? extends Mask<?, ?>>> classComboBox;

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public Transferable createTransferable(JComponent c) {
        if (c instanceof JList) {
            Class<? extends Mask<?, ?>> clazz = (Class<? extends Mask<?, ?>>) classComboBox.getSelectedItem();
            JList<Method> methodJList = (JList<Method>) c;
            Method method = methodJList.getSelectedValue();

            List<String> paramClassNames = Arrays.stream(method.getParameterTypes())
                                                 .map(Class::getCanonicalName)
                                                 .collect(Collectors.toList());

            return new GraphMethodTransferable(clazz.getCanonicalName(), method.getDeclaringClass().getCanonicalName(),
                                               method.getName(), paramClassNames);
        }

        return null;
    }
}
