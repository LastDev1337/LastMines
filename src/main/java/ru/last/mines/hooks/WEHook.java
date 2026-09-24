package ru.last.mines.hooks;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.last.mines.models.MineShape;

import java.util.ArrayList;
import java.util.List;

public class WEHook {

    public static Object[] captureSelection(Player player) {
        Region region = selectionOf(player);
        if (region == null) return null;
        MineShape shape = shapeOf(region);
        return new Object[]{shape, serializePoints(region, shape)};
    }

    private static Region selectionOf(Player player) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            if (session != null && session.getSelectionWorld() != null) {
                return session.getSelection(session.getSelectionWorld());
            }
        } catch (Throwable ignored) {
            // nope
        }
        return null;
    }

    private static MineShape shapeOf(Region region) {
        if (region instanceof CylinderRegion) return MineShape.CYLINDER;
        if (region instanceof Polygonal2DRegion) return MineShape.POLYGON;
        if (region instanceof EllipsoidRegion) return MineShape.ELLIPSOID;
        return MineShape.CUBOID;
    }

    private static List<String> serializePoints(Region region, MineShape shape) {
        List<String> out = new ArrayList<>();
        switch (shape) {
            case CYLINDER -> {
                CylinderRegion c = (CylinderRegion) region;
                Vector3 center = c.getCenter();
                Vector2 r = c.getRadius();
                out.add(center.x() + ";" + c.getMinimumY() + ";" + center.z());
                out.add(r.x() + ";0;" + r.z());
                out.add(c.getMinimumY() + ";0;" + c.getMaximumY());
            }
            case ELLIPSOID -> {
                EllipsoidRegion e = (EllipsoidRegion) region;
                Vector3 center = e.getCenter();
                Vector3 r = e.getRadius();
                out.add(center.x() + ";" + center.y() + ";" + center.z());
                out.add(r.x() + ";" + r.y() + ";" + r.z());
            }
            case POLYGON -> {
                Polygonal2DRegion p = (Polygonal2DRegion) region;
                for (BlockVector2 v : p.getPoints()) out.add(v.x() + ";0;" + v.z());
                out.add(p.getMinimumY() + ";0;" + p.getMaximumY());
            }
            default -> {
                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                out.add(min.x() + ";" + min.y() + ";" + min.z());
                out.add(max.x() + ";" + max.y() + ";" + max.z());
            }
        }
        return out;
    }

    public static Object buildRegion(World bukkitWorld, MineShape shape, List<String> points) {
        if (shape == MineShape.CUBOID || bukkitWorld == null || points == null || points.size() < 2) return null;
        try {
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(bukkitWorld);
            return switch (shape) {
                case CYLINDER -> {
                    double[] c = split(points.get(0));
                    double[] r = split(points.get(1));
                    double[] y = split(points.get(2));
                    yield new CylinderRegion(world, BlockVector3.at(c[0], c[1], c[2]), Vector2.at(r[0], r[2]), (int) y[0], (int) y[2]);
                }
                case ELLIPSOID -> {
                    double[] c = split(points.get(0));
                    double[] r = split(points.get(1));
                    yield new EllipsoidRegion(world, BlockVector3.at(c[0], c[1], c[2]), Vector3.at(r[0], r[1], r[2]));
                }
                case POLYGON -> {
                    double[] y = split(points.getLast());
                    List<BlockVector2> verts = new ArrayList<>();
                    for (int i = 0; i < points.size() - 1; i++) {
                        double[] p = split(points.get(i));
                        verts.add(BlockVector2.at(p[0], p[2]));
                    }
                    yield new Polygonal2DRegion(world, verts, (int) y[0], (int) y[2]);
                }
                default -> null;
            };
        } catch (Throwable t) {
            return null;
        }
    }

    private static double[] split(String s) {
        String[] parts = s.split(";");
        return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])};
    }

    public static boolean contains(Object regionObj, int x, int y, int z) {
        if (!(regionObj instanceof Region region)) return false;
        try {
            return region.contains(BlockVector3.at(x, y, z));
        } catch (Throwable t) {
            return false;
        }
    }

    public static List<int[]> points(Object regionObj) {
        List<int[]> list = new ArrayList<>();
        if (!(regionObj instanceof Region region)) return list;
        try {
            for (BlockVector3 v : region) list.add(new int[]{v.x(), v.y(), v.z()});
        } catch (Throwable ignored) {
        }
        return list;
    }

    public static int[] minPoint(Object regionObj) {
        if (!(regionObj instanceof Region region)) return null;
        BlockVector3 v = region.getMinimumPoint();
        return new int[]{v.x(), v.y(), v.z()};
    }

    public static int[] maxPoint(Object regionObj) {
        if (!(regionObj instanceof Region region)) return null;
        BlockVector3 v = region.getMaximumPoint();
        return new int[]{v.x(), v.y(), v.z()};
    }
}
