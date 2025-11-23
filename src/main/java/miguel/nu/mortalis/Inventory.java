package miguel.nu.mortalis;

import miguel.nu.mortalis.Main;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Inventory {

    public static boolean isPresent(ItemStack s) {
        return s != null && s.getType() != Material.AIR && s.getAmount() > 0;
    }

    public static boolean isEmpty(ItemStack s) {
        return s == null || s.getType() == Material.AIR || s.getAmount() <= 0;
    }

    private static ItemStack cloneOrNull(ItemStack s) {
        return s == null ? null : s.clone();
    }

    /**
     * inventoryItems (current player inventory) indices:
     *  0 - 35 : Main inventory
     *  36     : Offhand
     *  37     : Boots
     *  38     : Leggings
     *  39     : Chestplate
     *  40     : Helmet
     *
     * items (what we're trying to pick up), laid out the same way:
     *  0 - 35 : Main inventory contents from death/backpack/etc.
     *  36     : Offhand item from death
     *  37     : Boots from death
     *  38     : Leggings from death
     *  39     : Chestplate from death
     *  40     : Helmet from death
     *
     * Behavior:
     *  1) Simulate whether the main storage (slots 0–35) can hold:
     *     - all existing main-inventory items from inventoryItems[0–35], AND
     *     - all pickup main items from items[0–35],
     *     stacking where possible.
     *
     *  2) Then, in this order, handle equipment from the pickup (items[x]):
     *       - Boots  (index 37)
     *       - Legs   (index 38)
     *       - Chest  (index 39)
     *       - Helmet (index 40)
     *       - Offhand(index 36)
     *
     *     For each:
     *       - If there is no such item in items[x], skip it.
     *       - If the player's corresponding slot (inventoryItems[x]) is EMPTY:
     *           -> we can equip the new item for free (no main-slot usage).
     *       - If the player's slot is NOT EMPTY:
     *           -> we will equip the new item and the OLD item from
     *              inventoryItems[x] must fit into the main storage (0–35),
     *              using the same stacking rules.
     *
     *  3) If at any point we cannot fit something into the 0–35 main storage,
     *     return false. Otherwise return true.
     */
    public static boolean canFitAll(List<ItemStack> inventoryItems, List<ItemStack> items, boolean debug) {
        // --- 1) Simulate main storage with stacking ---

        // Start with an empty simulated main inventory (36 slots)
        List<ItemStack> simSlots = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            simSlots.add(null);
        }

        // a) Add current main inventory items (inventoryItems[0..35]) into simSlots with stacking
        if (inventoryItems != null) {
            int limit = Math.min(36, inventoryItems.size());
            for (int i = 0; i < limit; i++) {
                ItemStack cur = inventoryItems.get(i);
                if (!isPresent(cur)) continue;
                if (!fitOne(simSlots, cur.clone())) {
                    if (debug) {
                        Main.plugin.getLogger().info("Main inventory overflowed with existing item " + cur.getType());
                    }
                    return false;
                }
            }
        }

        // b) Add pickup main items (items[0..35]) into simSlots with stacking
        if (items != null) {
            int limit = Math.min(36, items.size());
            for (int i = 0; i < limit; i++) {
                ItemStack cur = items.get(i);
                if (!isPresent(cur)) continue;
                if (!fitOne(simSlots, cur.clone())) {
                    if (debug) {
                        Main.plugin.getLogger().info("Main inventory overflowed with pickup item " + cur.getType());
                    }
                    return false;
                }
            }
        }

        // --- 2) Handle equipment slots in the specified order ---

        // Helper to get a safe item from a list at an index
        java.util.function.BiFunction<List<ItemStack>, Integer, ItemStack> getAt =
                (list, idx) -> (list != null && idx < list.size()) ? list.get(idx) : null;

        // Boots (index 37)
        if (!handleEquipSlot(37, inventoryItems, items, simSlots, debug, "boots")) {
            return false;
        }

        // Legs (index 38)
        if (!handleEquipSlot(38, inventoryItems, items, simSlots, debug, "leggings")) {
            return false;
        }

        // Chest (index 39)
        if (!handleEquipSlot(39, inventoryItems, items, simSlots, debug, "chestplate")) {
            return false;
        }

        // Helmet (index 40)
        if (!handleEquipSlot(40, inventoryItems, items, simSlots, debug, "helmet")) {
            return false;
        }

        // Offhand (index 36)
        if (!handleEquipSlot(36, inventoryItems, items, simSlots, debug, "offhand")) {
            return false;
        }

        if (debug) {
            Main.plugin.getLogger().info("Player was able to fit all items (main + equipment)");
        }
        return true;
    }

    /**
     * Handle one equipment slot:
     * - newItem comes from items[slotIndex]
     * - existing equipped item comes from inventoryItems[slotIndex]
     * If existing equipped is non-empty and newItem is present, try to fit
     * the existing equipped item into the main inventory (simSlots).
     */
    private static boolean handleEquipSlot(
            int slotIndex,
            List<ItemStack> inventoryItems,
            List<ItemStack> items,
            List<ItemStack> simSlots,
            boolean debug,
            String label
    ) {
        ItemStack newItem = (items != null && slotIndex < items.size()) ? items.get(slotIndex) : null;
        if (!isPresent(newItem)) {
            // No new item for this slot from the pickup
            return true;
        }

        ItemStack existing = (inventoryItems != null && slotIndex < inventoryItems.size())
                ? inventoryItems.get(slotIndex)
                : null;

        if (isEmpty(existing)) {
            // Slot is empty → we can equip newItem for free, no main inventory usage.
            return true;
        }

        // Slot already has something → that existing item must fit into main inventory.
        ItemStack oldClone = existing.clone();
        if (!fitOne(simSlots, oldClone)) {
            if (debug) {
                Main.plugin.getLogger().info(
                        "Could not fit existing " + label + " item " + existing.getType() + " into main inventory");
            }
            return false;
        }

        return true;
    }

    /**
     * Try to fit a single stack into the given slots (main inventory simulation).
     * - First merges into similar stacks up to max stack size.
     * - Then fills empty slots with new stacks.
     */
    public static boolean fitOne(List<ItemStack> slots, ItemStack in) {
        if (!isPresent(in)) {
            return true;
        }

        int max = in.getMaxStackSize();

        // merge items into similar stacks
        for (int i = 0; i < slots.size() && in.getAmount() > 0; i++) {
            ItemStack cur = slots.get(i);
            if (!isPresent(cur)) continue;
            if (!cur.isSimilar(in)) continue;

            int space = max - cur.getAmount();
            if (space <= 0) continue;

            int move = Math.min(space, in.getAmount());
            cur.setAmount(cur.getAmount() + move);
            in.setAmount(in.getAmount() - move);
        }

        // place items into empties
        for (int i = 0; i < slots.size() && in.getAmount() > 0; i++) {
            ItemStack cur = slots.get(i);
            if (cur == null || cur.getType() == Material.AIR) {
                int place = Math.min(max, in.getAmount());
                ItemStack placed = in.clone();
                placed.setAmount(place);
                slots.set(i, placed);
                in.setAmount(in.getAmount() - place);
            }
        }

        return in.getAmount() <= 0;
    }
}
