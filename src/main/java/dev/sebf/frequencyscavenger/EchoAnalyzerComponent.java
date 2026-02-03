package dev.sebf.frequencyscavenger;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EchoAnalyzerComponent implements Component<EntityStore> {

    public static ComponentType<EntityStore, EchoAnalyzerComponent> getComponentType() {
        return FrequencyScavenger.get().getEchoAnalyzerComponentType();
    }

    private EchoAnalyzerUIHud hud;
    private double lastDistance = Double.MAX_VALUE;
    private float lastPing = 0;

    public EchoAnalyzerUIHud getHud() {
        return hud;
    }

    public void setHud(EchoAnalyzerUIHud hud) {
        this.hud = hud;
    }

    public double getLastDistance() {
        return lastDistance;
    }

    public void setLastDistance(double lastDistance) {
        this.lastDistance = lastDistance;
    }

    public float getLastPing(float dt) {
        lastPing += dt;
        return lastPing;
    }

    public void resetPing() {
        lastPing = 0;
    }

    @Override
    public Component<EntityStore> clone() {
        return new EchoAnalyzerComponent();
    }
}
