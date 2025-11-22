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

    /**
     * inventoryItems indices:
     *  0 - 35 : Main inventory
     *  36     : Boots
     *  37     : Leggings
     *  38     : Chestplate
     *  39     : Helmet
     *  40     : Offhand
     *
     * items:
     *  extra items you want to try to fit (e.g. grave/backpack)
     *
     * Behavior:
     *  - Only slots 0–35 are treated as storage (simulated inventory).
     *  - We FIRST virtually auto-equip armor/offhand items from `items`
     *    into empty armor/offhand slots (36–40).
     *  - Only the remaining items must fit into 0–35.
     *  - If everything fits into those 36 slots, returns true.
     */
    public static boolean canFitAll(List<ItemStack> inventoryItems, List<ItemStack> items, boolean debug) {
        // 1) Build simulated main inventory (0–35 only)
        int mainSize = Math.min(36, inventoryItems.size());
        List<ItemStack> simSlots = new ArrayList<>(mainSize);
        for (int i = 0; i < mainSize; i++) {
            ItemStack s = inventoryItems.get(i);
            simSlots.add(s == null ? null : s.clone());
        }

        // 2) Simulated armor/offhand slots (36–40) based on inventoryItems
        ItemStack boots   = inventoryItems.size() > 36 ? cloneOrNull(inventoryItems.get(36)) : null;
        ItemStack legs    = inventoryItems.size() > 37 ? cloneOrNull(inventoryItems.get(37)) : null;
        ItemStack chest   = inventoryItems.size() > 38 ? cloneOrNull(inventoryItems.get(38)) : null;
        ItemStack helmet  = inventoryItems.size() > 39 ? cloneOrNull(inventoryItems.get(39)) : null;
        ItemStack offhand = inventoryItems.size() > 40 ? cloneOrNull(inventoryItems.get(40)) : null;

        // 3) Build list of items that still need to fit in main inventory AFTER
        //    virtually equipping whatever we can into empty armor/offhand slots.
        List<ItemStack> toPlace = new ArrayList<>();

        if (items != null) {
            for (ItemStack original : items) {
                if (!isPresent(original)) continue;

                ItemStack in = original.clone();
                Material type = in.getType();

                boolean equipped = false;

                // Try to "equip" in simulation if the slot is empty and the item fits that slot
                if (isEmpty(boots) && isBoots(type)) {
                    boots = in;
                    equipped = true;
                } else if (isEmpty(legs) && isLeggings(type)) {
                    legs = in;
                    equipped = true;
                } else if (isEmpty(chest) && isChestplate(type)) {
                    chest = in;
                    equipped = true;
                } else if (isEmpty(helmet) && isHelmet(type)) {
                    helmet = in;
                    equipped = true;
                } else if (isEmpty(offhand) && canGoOffhand(type)) {
                    // Optional: you can restrict this to shields/totems if you want
                    offhand = in;
                    equipped = true;
                }

                // If we couldn't equip it, it must fit into main inventory
                if (!equipped) {
                    toPlace.add(in);
                }
            }
        }

        // 4) Try to fit remaining items into the simulated main inventory
        for (ItemStack in : toPlace) {
            if (!fitOne(simSlots, in)) {
                if (debug) {
                    Main.plugin.getLogger().info("Player could not fit item " + in.getType());
                }
                return false;
            }
        }

        if (debug) {
            Main.plugin.getLogger().info("Player was able to fit all items");
        }
        return true;
    }

    private static ItemStack cloneOrNull(ItemStack s) {
        return s == null ? null : s.clone();
    }

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

    // --- Helpers to classify armor/offhand items ---

    private static boolean isHelmet(Material m) {
        return switch (m) {
            case LEATHER_HELMET,
                 CHAINMAIL_HELMET,
                 IRON_HELMET,
                 GOLDEN_HELMET,
                 DIAMOND_HELMET,
                 NETHERITE_HELMET,
                 TURTLE_HELMET -> true;
            default -> false;
        };
    }

    private static boolean isChestplate(Material m) {
        return switch (m) {
            case LEATHER_CHESTPLATE,
                 CHAINMAIL_CHESTPLATE,
                 IRON_CHESTPLATE,
                 GOLDEN_CHESTPLATE,
                 DIAMOND_CHESTPLATE,
                 NETHERITE_CHESTPLATE -> true;
            default -> false;
        };
    }

    private static boolean isLeggings(Material m) {
        return switch (m) {
            case LEATHER_LEGGINGS,
                 CHAINMAIL_LEGGINGS,
                 IRON_LEGGINGS,
                 GOLDEN_LEGGINGS,
                 DIAMOND_LEGGINGS,
                 NETHERITE_LEGGINGS -> true;
            default -> false;
        };
    }

    private static boolean isBoots(Material m) {
        return switch (m) {
            case LEATHER_BOOTS,
                 CHAINMAIL_BOOTS,
                 IRON_BOOTS,
                 GOLDEN_BOOTS,
                 DIAMOND_BOOTS,
                 NETHERITE_BOOTS -> true;
            default -> false;
        };
    }

    private static boolean canGoOffhand(Material m) {
        // You can tighten this to SHIELD, TOTEM_OF_UNDYING, etc. if you want.
        // For now, allow anything in offhand when it's empty.
        return m != Material.AIR;
    }
}
