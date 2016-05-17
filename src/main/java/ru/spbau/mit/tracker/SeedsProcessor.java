package ru.spbau.mit.tracker;

import java.net.InetSocketAddress;
import java.util.*;

public class SeedsProcessor {

    private static final long DEAD_SEED_TIMEOUT_MILLS = 60 * 1000;

    private HashMap<Integer, Set<Seed>> currentUpdatedSeedsInfo = new HashMap<>();
    private HashMap<Seed, List<Integer>> seeds = new HashMap<>();

    public synchronized List<Seed> getSeedsFromFileId(int id) {
        if (!currentUpdatedSeedsInfo.containsKey(id)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(currentUpdatedSeedsInfo.get(id));
    }

    public synchronized void updateSeed(InetSocketAddress seedAddress, List<Integer> fileIds, long
            currentTimeMills) {
        fileIds.forEach(id -> addSeed(id, new Seed(seedAddress, currentTimeMills)));
    }

    public synchronized void updateOnTick(long currentTimeMills) {
        currentUpdatedSeedsInfo.values().forEach(x -> x.removeIf(seed -> isSeedDead(seed, currentTimeMills)));
        seeds.keySet().removeIf(seed -> isSeedDead(seed, currentTimeMills));
    }

    private void addSeed(int id, Seed seed) {
        if (!currentUpdatedSeedsInfo.containsKey(id)) {
            currentUpdatedSeedsInfo.put(id, new HashSet<>());
        }

        currentUpdatedSeedsInfo.get(id).add(seed);
    }

    private static boolean isSeedDead(Seed seed, long currentTimeMills) {
        return currentTimeMills - seed.lastUpdate >= DEAD_SEED_TIMEOUT_MILLS;
    }

    public static class Seed {
        private InetSocketAddress address;
        private long lastUpdate;

        public Seed(InetSocketAddress address, long lastUpdate) {
            this.address = address;
            this.lastUpdate = lastUpdate;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public void setAddress(InetSocketAddress address) {
            this.address = address;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

        @Override
        public boolean equals(Object obj) {
            return address.equals(((Seed) obj).address);
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }
    }
}
