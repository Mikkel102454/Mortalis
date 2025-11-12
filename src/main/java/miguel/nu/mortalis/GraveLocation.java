package miguel.nu.mortalis;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Find the nearest AIR block to a given location, within build limits. */
public final class GraveLocation {

    private GraveLocation() {}

    /** Convenience overload: search up to 16 blocks away. */
    public static Location findNearestAirBlock(Location base) {
        return findNearestAirBlock(base, 16);
    }

    /**
     * Finds the nearest AIR block to {@code base}, within world build height and the given radius.
     * Returns a block-aligned Location. If none found in range, returns the original block location.
     */
    public static Location findNearestAirBlock(Location base, int maxRadius) {
        World world = base.getWorld();
        if (world == null) return base;

        base = base.getBlock().getLocation();

        final int minY = world.getMinHeight();
        final int maxY = world.getMaxHeight() - 1;

        Block baseBlock = base.getBlock();
        if (isAir(baseBlock) && withinBuildLimits(baseBlock.getY(), minY, maxY)) {
            return baseBlock.getLocation();
        }

        ArrayDeque<Vector> q = new ArrayDeque<>();
        Set<Vector> seen = new HashSet<>();

        final int bx = base.getBlockX();
        final int by = clampY(base.getBlockY(), minY, maxY);
        final int bz = base.getBlockZ();

        Vector start = new Vector(bx, by, bz);
        q.add(start);
        seen.add(start);

        // Neighbor offsets (6-connected)
        final int[][] dirs = new int[][]{
                { 1,  0,  0},
                {-1,  0,  0},
                { 0,  1,  0},
                { 0, -1,  0},
                { 0,  0,  1},
                { 0,  0, -1}
        };

        final int maxR2 = maxRadius * maxRadius;

        while (!q.isEmpty()) {
            Vector v = q.pollFirst();
            int x = v.getBlockX();
            int y = v.getBlockY();
            int z = v.getBlockZ();

            if (!withinBuildLimits(y, minY, maxY)) continue;

            Block b = world.getBlockAt(x, y, z);
            if (isAir(b)) {
                return b.getLocation();
            }

            // Push neighbors
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                int nz = z + d[2];

                // Radius check (euclidean)
                int dx = nx - bx, dy = ny - by, dz = nz - bz;
                if (dx * dx + dy * dy + dz * dz > maxR2) continue;

                Vector nv = new Vector(nx, ny, nz);
                if (seen.add(nv)) {
                    q.addLast(nv);
                }
            }
        }

        return base;
    }

    private static boolean isAir(Block b) {
        return b != null && (b.isEmpty() || b.getType().isAir());
    }

    private static boolean withinBuildLimits(int y, int minY, int maxY) {
        return y >= minY && y <= maxY;
    }

    private static int clampY(int y, int minY, int maxY) {
        return Math.max(minY, Math.min(maxY, y));
    }
}