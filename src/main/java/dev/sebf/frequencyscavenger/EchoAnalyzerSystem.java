package dev.sebf.frequencyscavenger;

import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EchoAnalyzerSystem extends RefChangeSystem<EntityStore, EchoAnalyzerComponent> {
    private static final Query<EntityStore> QUERY = Query.and(PlayerRef.getComponentType(), EchoAnalyzerComponent.getComponentType());

    @Nonnull
    @Override
    public ComponentType<EntityStore, EchoAnalyzerComponent> componentType() {
        return EchoAnalyzerComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull EchoAnalyzerComponent echoAnalyzerComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        echoAnalyzerComponent.setHud(new EchoAnalyzerUIHud(playerRef));
        MultipleHUD.getInstance().setCustomHud(player, playerRef, "EchoAnalyzer", echoAnalyzerComponent.getHud());
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull EchoAnalyzerComponent echoAnalyzerComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (echoAnalyzerComponent.getHud() != null) {
            MultipleHUD.getInstance().hideCustomHud(player, "EchoAnalyzer");
            echoAnalyzerComponent.setHud(null);
        }
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nullable EchoAnalyzerComponent echoAnalyzerComponent, @Nonnull EchoAnalyzerComponent t1, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }
}
