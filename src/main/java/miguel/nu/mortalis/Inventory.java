package miguel.nu.mortalis;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Inventory {
    public static ItemStack norm(ItemStack s) {
        if (s == null) return null;
        if (s.getType() == Material.AIR) return null;
        if (s.getAmount() <= 0) return null;
        return s.clone();
    }

    public static boolean isPresent(ItemStack s) {
        return s != null && s.getType() != Material.AIR && s.getAmount() > 0;
    }

    public static ItemStack[] cloneArray(ItemStack[] arr) {
        ItemStack[] out = new ItemStack[arr.length];
        for (int i = 0; i < arr.length; i++) out[i] = (arr[i] == null ? null : arr[i].clone());
        return out;
    }

    public static boolean canFitAll(ItemStack[] storageSnapshot, List<ItemStack> items) {
        ItemStack[] slots = cloneArray(storageSnapshot);
        for (ItemStack in : items) {
            if (!fitOne(slots, in.clone())) return false;
        }
        return true;
    }

    public static boolean fitOne(ItemStack[] slots, ItemStack in) {
        int max = in.getMaxStackSize();

        // merge items into similar stacks
        for (int i = 0; i < slots.length && in.getAmount() > 0; i++) {
            ItemStack cur = slots[i];
            if (!isPresent(cur)) continue;
            if (!cur.isSimilar(in)) continue;
            int space = max - cur.getAmount();
            if (space <= 0) continue;
            int move = Math.min(space, in.getAmount());
            cur.setAmount(cur.getAmount() + move);
            in.setAmount(in.getAmount() - move);
        }

        // place items into empties
        for (int i = 0; i < slots.length && in.getAmount() > 0; i++) {
            if (slots[i] == null || slots[i].getType() == Material.AIR) {
                int place = Math.min(max, in.getAmount());
                ItemStack placed = in.clone();
                placed.setAmount(place);
                slots[i] = placed;
                in.setAmount(in.getAmount() - place);
            }
        }
        return in.getAmount() <= 0;
    }
}
