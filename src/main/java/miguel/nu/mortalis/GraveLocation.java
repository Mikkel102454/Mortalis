package miguel.nu.mortalis;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class GraveLocation {

    private GraveLocation() {}

    /**
     * Rules:
     *
     * - If the player is in liquid (feet OR head in liquid):
     *   - Scan straight up for the first AIR block.
     *   - Stop as soon as we hit a solid block (anything that is not liquid and not air).
     *   - If no AIR is found before that (or before build height) -> spawn at the player's feet.
     *
     * - If the player is NOT in liquid:
     *   - If something that is not liquid or air is there, move one block up,
     *     and keep going up until you find AIR.
     *   - If no AIR is found at all -> spawn at the player's feet.
     */
    public static Location findGraveLocation(Location base) {
        World world = base.getWorld();
        if (world == null) return base;

        // Work with block-aligned coordinates
        Block feetBlock = base.getBlock();
        int x = feetBlock.getX();
        int y = feetBlock.getY();
        int z = feetBlock.getZ();
        int maxY = world.getMaxHeight() - 1;

        boolean inLiquid = isInLiquid(base);

        // CASE 1: Player is in liquid (feet OR head in liquid)
        if (inLiquid) {
            // Find first air above, stopping when we hit a solid (non-air, non-liquid)
            Location airAbove = findFirstAirAboveUntilSolid(world, x, y, z, maxY);
            if (airAbove != null) {
                return airAbove;
            }

            // No air before solid/build height -> spawn at player's feet
            return findFirstAirOrLiquidUpwards(world, x, y, z, maxY);
        }

        // CASE 2: Player is NOT in liquid
        // "if something that is not liquid or air is there it should move a block up and so on"
        Location firstAirUp = findFirstAirOrLiquidUpwards(world, x, y, z, maxY);
        if (firstAirUp != null) {
            return firstAirUp;
        }

        // No air found above -> fall back to feet
        return feetBlock.getLocation();
    }

    /**
     * Determine if the player is "in liquid":
     * - feet block is liquid OR
     * - head block is liquid
     */
    private static boolean isInLiquid(Location base) {
        World world = base.getWorld();
        if (world == null) return false;

        Block feet = base.getBlock();
        Block head = base.clone().add(0, 1, 0).getBlock();

        return (feet != null && feet.isLiquid()) ||
                (head != null && head.isLiquid());
    }

    /**
     * For the "in liquid" case:
     * Scan upwards starting ABOVE startY:
     *  - Return the first AIR block found.
     *  - Stop and return null if we hit a solid (non-air, non-liquid) block.
     */
    private static Location findFirstAirAboveUntilSolid(World world, int x, int startY, int z, int maxY) {
        for (int yy = startY + 1; yy <= maxY; yy++) {
            Block b = world.getBlockAt(x, yy, z);

            if (isAir(b)) {
                return b.getLocation();
            }

            // Solid = not air and not liquid
            if (!b.isLiquid() && !isAir(b)) {
                // Hit a solid block before finding air
                return null;
            }

            // If it's liquid, keep going up
        }
        return null; // reached build height with no air
    }

    /**
     * For the "move a block up and so on" case:
     * Start at startY and go up until maxY, returning the first AIR block.
     * This never places the grave in a non-air block (heads, slabs, graves, etc.).
     */
    private static Location findFirstAirUpwards(World world, int x, int startY, int z, int maxY) {
        for (int yy = startY; yy <= maxY; yy++) {
            Block b = world.getBlockAt(x, yy, z);
            if (isAir(b)) {
                return b.getLocation();
            }
        }
        return null;
    }

    private static Location findFirstAirOrLiquidUpwards(World world, int x, int startY, int z, int maxY) {
        for (int yy = startY; yy <= maxY; yy++) {
            Block b = world.getBlockAt(x, yy, z);
            if (isAir(b) || b.isLiquid()) {
                return b.getLocation();
            }
        }
        return null;
    }


    private static boolean isAir(Block b) {
        return b != null && (b.isEmpty() || b.getType().isAir());
    }
}
