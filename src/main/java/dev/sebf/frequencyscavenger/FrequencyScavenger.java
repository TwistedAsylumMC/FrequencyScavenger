package dev.sebf.frequencyscavenger;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Random;

public class FrequencyScavenger extends JavaPlugin {
    private static final int CHUNK_SIZE = 128;

    private static FrequencyScavenger instance;

    private FrequencyScavengerData data;
    private ComponentType<EntityStore, EchoAnalyzerComponent> echoAnalyzerComponentType;

    public FrequencyScavenger(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void start() {
        data = FrequencyScavengerData.load();

        echoAnalyzerComponentType = getEntityStoreRegistry().registerComponent(EchoAnalyzerComponent.class, EchoAnalyzerComponent::new);
        getEntityStoreRegistry().registerSystem(new EchoAnalyzerSystem());
        getEntityStoreRegistry().registerSystem(new EchoAnalyzerTickingSystem());
    }

    public static FrequencyScavenger get() {
        return instance;
    }

    public FrequencyScavengerData getData() {
        return data;
    }

    public Vector3i getNearestSignal(Ref<EntityStore> ref, Store<EntityStore> store, int radius) {
        World world = store.getExternalData().getWorld();
        TransformComponent transform = Objects.requireNonNull(store.getComponent(ref, TransformComponent.getComponentType()));
        Vector3i playerPos = transform.getPosition().toVector3i();
        int px = (int) Math.floor(transform.getPosition().getX() / CHUNK_SIZE);
        int pz = (int) Math.floor(transform.getPosition().getZ() / CHUNK_SIZE);

        Vector3i closest = null;
        double closestDist = Float.MAX_VALUE;
        for (int x = px - radius; x <= px + radius; x++) {
            for (int z = pz - radius; z <= pz + radius; z++) {
                Vector3i signal = getSignalInChunk(world, x, z);
                if (!data.hasFoundSignal(signal)) {
                    double dist = playerPos.distanceTo(signal);
                    if (dist < closestDist) {
                        closest = signal;
                        closestDist = dist;
                    }
                }
            }
        }
        return closest;
    }

    private Vector3i getSignalInChunk(World world, int cx, int cz) {
        long worldSeed = world.getWorldConfig().getSeed();
        long chunkSeed = worldSeed ^ ((long) cx << 32) ^ cz;
        Random rand = new Random(chunkSeed);

        int x = (cx * CHUNK_SIZE) + rand.nextInt(CHUNK_SIZE);
        int z = (cz * CHUNK_SIZE) + rand.nextInt(CHUNK_SIZE);
        return new Vector3i(x, 0, z);
    }

    public boolean isHoldingAnalyzer(Player player) {
        ItemStack heldItem = player.getInventory().getItemInHand();
        if (heldItem != null && heldItem.getItemId().equals("Echo_Analyzer")) return true;
        ItemStack utilityItem = player.getInventory().getUtilityItem();
        return utilityItem != null && utilityItem.getItemId().equals("Echo_Analyzer");
    }

    public ComponentType<EntityStore, EchoAnalyzerComponent> getEchoAnalyzerComponentType() {
        return echoAnalyzerComponentType;
    }
}