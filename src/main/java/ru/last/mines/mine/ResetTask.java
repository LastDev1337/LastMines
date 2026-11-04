package ru.last.mines.mine;

import dev.laststudio.lib.api.scheduler.SchedulerService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

final class ResetTask {

    private static final int MAX_BLOCKS_PER_STEP = 10_000;
    private static final long MAX_NANOS_PER_STEP = 10_000_000L;
    private static final int CLOCK_CHECK_EVERY = 256;

    private final Plugin plugin;
    private final SchedulerService scheduler;
    private final World world;
    private final IntFunction<Material> source;
    private final IntConsumer onDone;
    private final Deque<Part> parts = new ArrayDeque<>();
    private final AtomicInteger placed = new AtomicInteger();
    private volatile boolean cancelled;

    private ResetTask(Plugin plugin, SchedulerService scheduler, World world,
                      IntFunction<Material> source, IntConsumer onDone) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.world = world;
        this.source = source;
        this.onDone = onDone;
    }

    static ResetTask cuboid(Plugin plugin, SchedulerService scheduler, World world,
                            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                            IntFunction<Material> source, IntConsumer onDone) {
        ResetTask task = new ResetTask(plugin, scheduler, world, source, onDone);
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                task.parts.add(new CuboidPart(cx, cz,
                        Math.max(minX, cx << 4), Math.min(maxX, (cx << 4) + 15),
                        Math.max(minZ, cz << 4), Math.min(maxZ, (cz << 4) + 15),
                        minX, minY, minZ, maxY, sizeY, sizeZ));
            }
        }
        return task;
    }

    static ResetTask points(Plugin plugin, SchedulerService scheduler, World world, List<int[]> points,
                            IntFunction<Material> source, IntConsumer onDone) {
        ResetTask task = new ResetTask(plugin, scheduler, world, source, onDone);
        Map<Long, int[]> counts = new LinkedHashMap<>();
        for (int[] p : points) {
            counts.computeIfAbsent(chunkKey(p[0] >> 4, p[2] >> 4), k -> new int[1])[0]++;
        }
        Map<Long, int[]> indices = new HashMap<>();
        Map<Long, int[]> filled = new HashMap<>();
        for (Map.Entry<Long, int[]> e : counts.entrySet()) {
            indices.put(e.getKey(), new int[e.getValue()[0]]);
            filled.put(e.getKey(), new int[1]);
        }
        for (int i = 0; i < points.size(); i++) {
            int[] p = points.get(i);
            long key = chunkKey(p[0] >> 4, p[2] >> 4);
            indices.get(key)[filled.get(key)[0]++] = i;
        }
        for (Map.Entry<Long, int[]> e : indices.entrySet()) {
            long key = e.getKey();
            task.parts.add(new PointsPart((int) (key >> 32), (int) key, e.getValue(), points));
        }
        return task;
    }

    void start() {
        next();
    }

    void cancel() {
        cancelled = true;
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private void next() {
        if (cancelled) return;
        Part part = parts.peek();
        if (part == null) {
            scheduler.global(plugin, () -> {
                if (!cancelled) onDone.accept(placed.get());
            });
            return;
        }
        scheduler.region(plugin, part.location(world), () -> runPart(part));
    }

    private void runPart(Part part) {
        if (cancelled) return;
        long deadline = System.nanoTime() + MAX_NANOS_PER_STEP;
        if (part.step(this, deadline)) {
            parts.poll();
            next();
        } else {
            scheduler.regionLater(plugin, part.location(world), () -> runPart(part), 1L);
        }
    }

    private void place(int x, int y, int z, int index) {
        Material mat = source.apply(index);
        if (mat == null) return;
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != mat) block.setType(mat, false);
        if (mat != Material.AIR) placed.incrementAndGet();
    }

    private static boolean outOfBudget(int doneInStep, long deadline) {
        return doneInStep >= MAX_BLOCKS_PER_STEP
                || (doneInStep % CLOCK_CHECK_EVERY == 0 && System.nanoTime() > deadline);
    }

    private abstract static class Part {
        final int cx;
        final int cz;

        Part(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }

        Location location(World world) {
            return new Location(world, (cx << 4) + 8, 64, (cz << 4) + 8);
        }

        abstract boolean step(ResetTask task, long deadline);
    }

    private static final class CuboidPart extends Part {
        private final int x1, z0, z1, minX, minY, minZ, maxY, sizeY, sizeZ;
        private int x, y, z;

        CuboidPart(int cx, int cz, int x0, int x1, int z0, int z1,
                   int minX, int minY, int minZ, int maxY, int sizeY, int sizeZ) {
            super(cx, cz);
            this.x1 = x1;
            this.z0 = z0;
            this.z1 = z1;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxY = maxY;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.x = x0;
            this.y = minY;
            this.z = z0;
        }

        @Override
        boolean step(ResetTask task, long deadline) {
            int done = 0;
            while (x <= x1) {
                while (y <= maxY) {
                    while (z <= z1) {
                        task.place(x, y, z, ((x - minX) * sizeY + (y - minY)) * sizeZ + (z - minZ));
                        z++;
                        done++;
                        if (outOfBudget(done, deadline)) return false;
                    }
                    z = z0;
                    y++;
                }
                y = minY;
                x++;
            }
            return true;
        }
    }

    private static final class PointsPart extends Part {
        private final int[] indices;
        private final List<int[]> points;
        private int cursor;

        PointsPart(int cx, int cz, int[] indices, List<int[]> points) {
            super(cx, cz);
            this.indices = indices;
            this.points = points;
        }

        @Override
        boolean step(ResetTask task, long deadline) {
            int done = 0;
            while (cursor < indices.length) {
                int index = indices[cursor++];
                int[] p = points.get(index);
                task.place(p[0], p[1], p[2], index);
                done++;
                if (outOfBudget(done, deadline)) return cursor >= indices.length;
            }
            return true;
        }
    }
}
