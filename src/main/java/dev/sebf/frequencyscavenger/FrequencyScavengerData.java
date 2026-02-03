package dev.sebf.frequencyscavenger;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.util.BsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FrequencyScavengerData {
    private static final BuilderCodec<FrequencyScavengerData> CODEC = BuilderCodec.builder(FrequencyScavengerData.class, FrequencyScavengerData::new)
            .append(new KeyedCodec<>("Signals", new ArrayCodec<>(Vector3i.CODEC, Vector3i[]::new)), (d, v) -> {
                if (d.foundSignals != null) {
                    Collections.addAll(d.foundSignals, v);
                }
            }, d -> d.foundSignals.toArray(Vector3i[]::new))
            .add()
            .build();
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Path DATA_PATH = Constants.UNIVERSE_PATH.resolve("frequency_scavenger.json");

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<Vector3i> foundSignals = new HashSet<>();

    public static FrequencyScavengerData load() {
        try {
            if (Files.exists(DATA_PATH)) {
                return RawJsonReader.readSync(DATA_PATH, CODEC, LOGGER);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new FrequencyScavengerData();
    }

    public void save() {
        lock.readLock().lock();
        try {
            BsonUtil.writeSync(DATA_PATH, CODEC, this, LOGGER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasFoundSignal(Vector3i signal) {
        lock.readLock().lock();
        try {
            return foundSignals.contains(signal);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addFoundSignal(Vector3i signal) {
        lock.writeLock().lock();
        try {
            foundSignals.add(signal);
            BsonUtil.writeSync(DATA_PATH, CODEC, this, LOGGER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
