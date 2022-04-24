package com.faforever.neroxis.ui.components;

import java.util.Arrays;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;

public class ScrollMenuKeyListener implements MenuKeyListener {
    @Override
    public void menuKeyTyped(MenuKeyEvent e) {
    }

    @Override
    public void menuKeyPressed(MenuKeyEvent e) {
        if (e.getComponent() instanceof JScrollMenu) {
            JScrollMenu jScrollMenu = (JScrollMenu) e.getComponent();
            Arrays.stream(jScrollMenu.getMenuComponents())
                  .filter(component -> component instanceof JMenuItem)
                  .map(component -> (JMenuItem) component)
                  .filter(menuItem -> ((String) menuItem.getAction()
                                                        .getValue(Action.NAME)).startsWith(String.valueOf(e.getKeyChar())))
                  .findFirst()
                  .ifPresent(jMenuItem -> jScrollMenu.getPopupMenu().setSelected(jMenuItem));
        }
    }

    @Override
    public void menuKeyReleased(MenuKeyEvent e) {
    }
}
