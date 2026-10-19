package com.manage.nhakhoa.desktop.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/** Icon Java2D không phụ thuộc font ký hiệu hoặc file ảnh ngoài. */
public final class UiIcons implements Icon {
    private final String kind;
    private final Color color;
    private final int size;
    public UiIcons(String kind, Color color, int size) { this.kind = kind; this.color = color; this.size = size; }
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
    @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y); g.scale(size / 24.0, size / 24.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.setColor(color);
        switch (kind) {
            case "grid" -> { for (int a : new int[]{4, 14}) for (int b : new int[]{4, 14}) g.drawRoundRect(a, b, 6, 6, 1, 1); }
            case "calendar" -> { g.drawRoundRect(4, 5, 16, 16, 3, 3); g.drawLine(4, 10, 20, 10); g.drawLine(8, 3, 8, 7); g.drawLine(16, 3, 16, 7); }
            case "tooth" -> {
                Path2D p = new Path2D.Double(); p.moveTo(12, 5); p.curveTo(1, -1, 2, 12, 7, 21);
                p.curveTo(10, 24, 9, 12, 12, 13); p.curveTo(15, 12, 14, 24, 17, 21);
                p.curveTo(22, 12, 23, -1, 12, 5); g.draw(p);
            }
            case "wallet" -> { g.drawRoundRect(3, 7, 18, 14, 3, 3); g.drawLine(4, 7, 16, 3); g.drawRoundRect(14, 11, 8, 6, 1, 1); g.fillOval(16, 13, 2, 2); }
            case "person" -> { g.drawOval(8, 3, 8, 8); g.drawArc(4, 13, 16, 16, 0, 180); }
            case "lock" -> { g.drawRoundRect(5, 10, 14, 12, 2, 2); g.drawArc(8, 2, 8, 14, 0, 180); g.drawLine(12, 14, 12, 17); }
            case "medical" -> { g.drawRoundRect(3, 6, 18, 15, 3, 3); g.drawLine(8, 6, 8, 3); g.drawLine(8, 3, 16, 3); g.drawLine(16, 3, 16, 6); g.drawLine(12, 10, 12, 17); g.drawLine(8, 13, 16, 13); }
            case "box" -> { g.drawPolygon(new int[]{3,12,21,21,12,3}, new int[]{7,2,7,17,22,17},6); g.drawLine(3,7,12,12); g.drawLine(12,12,21,7); g.drawLine(12,12,12,22); }
            case "exchange" -> { g.drawLine(3,8,21,8); g.drawLine(17,4,21,8); g.drawLine(17,12,21,8); g.drawLine(3,17,21,17); g.drawLine(3,17,7,13); g.drawLine(3,17,7,21); }
            case "clock", "reload" -> { g.drawArc(3,3,18,18,40,295); g.drawLine(19,2,19,7); g.drawLine(19,7,14,7); g.drawLine(12,7,12,12); g.drawLine(12,12,16,14); }
            case "bell" -> { g.drawArc(6,3,12,14,0,180); g.drawLine(6,10,5,18); g.drawLine(18,10,19,18); g.drawLine(5,18,19,18); g.drawArc(10,19,4,3,180,180); }
            case "check" -> { g.drawLine(4,12,9,18); g.drawLine(9,18,20,5); }
            case "alert" -> { g.drawOval(3,3,18,18); g.drawLine(12,7,12,13); g.fillOval(11,16,2,2); }
            default -> { g.drawRoundRect(5,2,14,20,2,2); g.drawLine(8,7,16,7); g.drawLine(8,11,16,11); g.drawLine(8,15,14,15); }
        }
        g.dispose();
    }
}
