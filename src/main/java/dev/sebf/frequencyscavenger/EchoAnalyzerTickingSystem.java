package dev.sebf.frequencyscavenger;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import javax.annotation.Nonnull;
import java.util.Objects;

public class EchoAnalyzerTickingSystem extends EntityTickingSystem<EntityStore> {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();

    @Override
    public void tick(float dt, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = Objects.requireNonNull(archetypeChunk.getComponent(i, PlayerRef.getComponentType()));
        Ref<EntityStore> ref = Objects.requireNonNull(playerRef.getReference());
        Player player = Objects.requireNonNull(store.getComponent(ref, Player.getComponentType()));

        EchoAnalyzerComponent echoAnalyzerComponent = store.getComponent(ref, EchoAnalyzerComponent.getComponentType());
        boolean holding = FrequencyScavenger.get().isHoldingAnalyzer(player);

        if (!holding) {
            if (echoAnalyzerComponent != null) {
                commandBuffer.removeComponent(ref, EchoAnalyzerComponent.getComponentType());
            }
            return;
        } else if (echoAnalyzerComponent == null) {
            commandBuffer.addComponent(ref, EchoAnalyzerComponent.getComponentType());
            return;
        }
        updateSignal(playerRef, ref, store, echoAnalyzerComponent, dt);
    }

    private void updateSignal(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, EchoAnalyzerComponent echoAnalyzerComponent, float dt) {
        TransformComponent transform = Objects.requireNonNull(store.getComponent(ref, TransformComponent.getComponentType()));
        EchoAnalyzerUIHud hud = echoAnalyzerComponent.getHud();
        Vector3i nearestSignal = FrequencyScavenger.get().getNearestSignal(ref, store, 3);
        Vector3d playerPos = transform.getPosition();

        double lastDistance = echoAnalyzerComponent.getLastDistance();
        double distance = Double.MAX_VALUE;
        double lookAlignment = 0;

        if (nearestSignal != null) {
            float yaw = playerRef.getHeadRotation().getYaw();
            Vector3d playerDirection = new Vector3d(-Math.sin(yaw), 0, -Math.cos(yaw));

            Vector3d signalPos = nearestSignal.toVector3d().add(0.5, 0, 0.5);
            Vector3d signalDirection = signalPos.clone().subtract(playerPos);
            signalDirection.setY(0);
            signalDirection.normalize();

            distance = playerPos.distanceTo(new Vector3d(signalPos.x, playerPos.y, signalPos.z));
            lookAlignment = (float) playerDirection.dot(signalDirection);

            if (distance <= 3 && lookAlignment > 0.3) {
                discoverSignal(ref, store, nearestSignal);
            } else if (distance < 100) {
                if (lastDistance > 100) {
                    echoAnalyzerComponent.resetPing();
                    int soundId = SoundEvent.getAssetMap().getIndex("SFX_5sf_SignalFound");
                    SoundUtil.playSoundEvent2dToPlayer(playerRef, soundId, SoundCategory.UI);
                } else {
                    float normalizedDist = (float) (distance / 100.0);
                    float dynamicInterval = 1f + (normalizedDist * 4.5f);

                    if (echoAnalyzerComponent.getLastPing(dt) >= dynamicInterval) {
                        echoAnalyzerComponent.resetPing();
                        float pitch = 1.1f - (normalizedDist * 0.5f);
                        float volume = 0.3f + ((float)lookAlignment * 0.4f);
                        Vector3d pingPos = distance < 16 ? signalPos : playerPos.clone().add(new Vector3d(signalDirection.x * 16, 0, signalDirection.z * 16));
                        int soundId = SoundEvent.getAssetMap().getIndex("SFX_5sf_Ping");
                        SoundUtil.playSoundEvent3dToPlayer(ref, soundId, SoundCategory.SFX, pingPos.x, pingPos.y, pingPos.z, volume, pitch, store);
                    }
                }
            }
        }

        if (lastDistance < 100 && distance > 100) {
            echoAnalyzerComponent.resetPing();
            int soundId = SoundEvent.getAssetMap().getIndex("SFX_5sf_SignalLost");
            SoundUtil.playSoundEvent2dToPlayer(playerRef, soundId, SoundCategory.UI);
        }

        echoAnalyzerComponent.setLastDistance(distance);
        hud.updateSignal(dt, distance, lookAlignment);
    }

    private void discoverSignal(Ref<EntityStore> ref, Store<EntityStore> store, Vector3i signal) {
        PlayerRef playerRef = Objects.requireNonNull(store.getComponent(ref, PlayerRef.getComponentType()));
        Player player = Objects.requireNonNull(store.getComponent(ref, Player.getComponentType()));
        FrequencyScavenger.get().getData().addFoundSignal(signal);

        String[] rewards = new String[]{
                "Furniture_Construction_Sign",
                "Weapon_Assault_Rifle",
                "Weapon_Gun",
                "Weapon_Gun_Blunderbuss",
                "Weapon_Gun_Blunderbuss_Rusty",
                "Weapon_Handgun",
                "Memory_Particle",
                "Ingredient_Fire_Essence",
                "Ingredient_Ice_Essence",
                "Ingredient_Life_Essence",
                "Ingredient_Lightning_Essence",
                "Ingredient_Void_Essence",
                "Ingredient_Water_Essence",
                "Ingredient__Essence"
        };
        String reward = rewards[(int) (Math.random() * rewards.length)];
        ItemStack rewardItem = new ItemStack(reward);
        ItemContainer container = new CombinedItemContainer(player.getInventory().getHotbar(), player.getInventory().getStorage(), player.getInventory().getBackpack());
        ItemStackTransaction transaction = container.addItemStack(rewardItem);
        if (transaction.getRemainder() != null) {
            ItemUtils.dropItem(ref, transaction.getRemainder(), store);
        }

        int soundId = SoundEvent.getAssetMap().getIndex("SFX_5sf_EchoRecovered");
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundId, SoundCategory.UI);
        EventTitleUtil.showEventTitleToPlayer(playerRef, Message.translation(rewardItem.getItem().getTranslationKey()), Message.translation("frequencyscavenger.title.echo_recovered.subtitle"), true);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }
}
