package com.faforever.neroxis.ui.listener;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LostFocusListener implements FocusListener {

    private final Runnable lostFocusRunnable;

    @Override
    public void focusGained(FocusEvent e) {

    }

    @Override
    public void focusLost(FocusEvent e) {
        lostFocusRunnable.run();
    }
}
