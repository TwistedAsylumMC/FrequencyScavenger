package dev.sebf.frequencyscavenger;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class EchoAnalyzerUIHud extends CustomUIHud {
    private final int GRAPH_WIDTH = 394;
    private final int GRAPH_HEIGHT = 96;

    private final int[] heightHistory = new int[150];
    private final int[] widthHistory = new int[150];

    public float deltaTime = 0f;
    public double distanceToCache = 100f;
    public double lookAlignment = 0f;

    private float scrollTimer = 0f;

    public EchoAnalyzerUIHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Pages/EchoAnalyzer.ui");
        buildPage(uiCommandBuilder);
    }

    private void buildPage(@Nonnull UICommandBuilder uiCommandBuilder) {
        double intensity = 1.0f - Math.min(distanceToCache / 100f, 1.0f);
        double signalFloor = 0.2f;
        double lookBonus = lookAlignment > 0 ? lookAlignment * (1.0f - signalFloor) : 0;
        double directionalIntensity = intensity * (signalFloor + lookBonus);

        scrollTimer += deltaTime;
        if (scrollTimer >= 0.04f) {
            scrollTimer = 0;
            updateHistory(directionalIntensity);
        }

        drawLine(uiCommandBuilder, 0, GRAPH_HEIGHT - 5, GRAPH_WIDTH, 1, "#00FFFF(0.15)");

        uiCommandBuilder.clear("#Graph");
        renderGraph(uiCommandBuilder, 1, "#0066FF(0.4)");
        renderGraph(uiCommandBuilder, 0, "#00FFFF");

        String key = "no_signal";
        if (distanceToCache < 100) {
            key = directionalIntensity > 0.5 ? "strong_signal" : "weak_signal";
        }
        uiCommandBuilder.set("#StatusLabel.Text", Message.translation("frequencyscavenger.customUI.analyzer." + key));

        double rawProgress = (1.0 - Math.min(distanceToCache / 100.0, 1.0)) * lookAlignment;
        double confidencePercent = Math.max(0, rawProgress) * 100;
        uiCommandBuilder.set("#PercentLabel.Text", distanceToCache < 100 ? " (%.0f%%)".formatted(confidencePercent) : "");
    }

    private void updateHistory(double intensity) {
        System.arraycopy(heightHistory, 1, heightHistory, 0, heightHistory.length - 1);
        System.arraycopy(widthHistory, 1, widthHistory, 0, widthHistory.length - 1);

        int newHeight = (int) ((Math.random() * intensity) * GRAPH_HEIGHT);
        if (Math.random() > 0.5 + (intensity * 0.45)) newHeight = 0;
        int newWidth = 3 + (int)(intensity * 9);

        heightHistory[heightHistory.length - 1] = newHeight;
        widthHistory[widthHistory.length - 1] = newWidth;
    }

    private void renderGraph(UICommandBuilder uiCommandBuilder, int xOffset, String hexColor) {
        int currentX = GRAPH_WIDTH + xOffset;
        for (int i = heightHistory.length - 1; i > 0; i--) {
            int w = widthHistory[i];
            int h = heightHistory[i];
            int prevH = heightHistory[i - 1];

            currentX -= w;
            if (currentX < -w) break;

            int drawX = Math.max(0, currentX);
            int drawW = (currentX < 0) ? w + currentX : w;

            int currentY = GRAPH_HEIGHT - h;
            int prevY = GRAPH_HEIGHT - prevH;

            drawLine(uiCommandBuilder, drawX, currentY, drawW, 1, hexColor);

            int vertY = Math.min(currentY, prevY);
            int vertHeight = Math.abs(currentY - prevY);
            if (vertHeight > 0 && currentX > 0) {
                drawLine(uiCommandBuilder, currentX, vertY, 1, vertHeight, hexColor);
            }
        }
    }

    private void drawLine(UICommandBuilder uiCommandBuilder, int x, int y, int width, int height, String colour) {
        uiCommandBuilder.appendInline("#Graph", "Group { Anchor: (Left: %d, Top: %d, Width: %d, Height: %d); Background: %s; }".formatted(x, y, width, height, colour));
    }

    public void updateSignal(float dt, double dist, double la) {
        deltaTime = dt;
        distanceToCache = dist;
        lookAlignment = la;

        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        buildPage(uiCommandBuilder);
        update(false, uiCommandBuilder);
    }
}
