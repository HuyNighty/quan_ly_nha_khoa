package com.manage.nhakhoa.desktop.screens;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Gọi một hành động mỗi khi nội dung ô nhập thay đổi. */
final class SimpleDocumentListener implements DocumentListener {
    private final Runnable action;
    private SimpleDocumentListener(Runnable action) { this.action = action; }
    static DocumentListener of(Runnable action) { return new SimpleDocumentListener(action); }
    @Override public void insertUpdate(DocumentEvent e) { action.run(); }
    @Override public void removeUpdate(DocumentEvent e) { action.run(); }
    @Override public void changedUpdate(DocumentEvent e) { action.run(); }
}
