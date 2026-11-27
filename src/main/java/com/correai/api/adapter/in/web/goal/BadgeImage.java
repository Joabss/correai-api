package com.correai.api.adapter.in.web.goal;

import com.correai.api.domain.model.goal.Goal;
import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

final class BadgeImage {
    private BadgeImage() {
    }

    static byte[] render(Goal goal) {
        BufferedImage image = new BufferedImage(192, 216, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(34, 98, 160));
            g.fillPolygon(new int[]{45, 83, 103, 65}, new int[]{0, 0, 94, 94}, 4);
            g.setColor(new Color(45, 131, 193));
            g.fillPolygon(new int[]{109, 147, 127, 89}, new int[]{0, 0, 94, 94}, 4);
            g.setPaint(new GradientPaint(24, 50, new Color(255, 225, 125),
                    168, 200, new Color(183, 121, 18)));
            g.fillOval(16, 40, 160, 160);
            g.setColor(new Color(132, 82, 12));
            g.setStroke(new BasicStroke(4));
            g.drawOval(28, 52, 136, 136);
            String label = switch (goal.type()) {
                case DISTANCE_KM -> "KM";
                case ACTIVITIES -> "ATIV.";
                case AVG_PACE -> "PACE";
            };
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            centered(g, label, 107);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            String target = goal.type() == com.correai.api.domain.model.goal.GoalType.AVG_PACE
                    ? com.correai.api.adapter.in.web.activity.PaceFormatter.format((int) goal.target())
                    : java.math.BigDecimal.valueOf(goal.target()).stripTrailingZeros().toPlainString().replace('.', ',');
            centered(g, target, 140);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            centered(g, goal.period() == com.correai.api.domain.model.goal.GoalPeriod.WEEKLY
                    ? "SEMANAL" : "MENSAL", 166);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", out)) throw new IllegalStateException("PNG encoder unavailable");
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render badge", e);
        }
    }

    private static void centered(Graphics2D g, String text, int y) {
        g.drawString(text, (192 - g.getFontMetrics().stringWidth(text)) / 2, y);
    }
}
