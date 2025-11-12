package miguel.nu.mortalis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class Decay {

    /** Linear decay from safetyTime → (safetyTime + expireDuration). */
    public static double decayFraction(int timeLived, int safetyTime, int expireDuration) {
        if (timeLived <= safetyTime) return 0.0;
        if (expireDuration <= 0) return 1.0;
        int end = safetyTime + expireDuration;
        if (timeLived >= end) return 1.0;
        return (double) (timeLived - safetyTime) / (double) expireDuration;
    }

    /** Unified decay:
     *  - Damageable (durability): wear toward max-1 with p; never break at p<1
     *  - Stackable: partial stack loss via binomial keep with prob 1-p
     *  - Others: delete with prob p
     */
    public static ItemStack applyDecay(ItemStack in, double p) {
        if (in == null || in.getType() == Material.AIR || in.getAmount() <= 0) return null;
        if (p <= 0) return in.clone();
        if (p >= 1) return null;

        ItemStack s = in.clone();
        Material type = s.getType();

        if (isDamageable(s)) {
            ItemMeta meta = s.getItemMeta();
            Damageable dm = (Damageable) meta;
            int max = type.getMaxDurability();
            int cur = dm.getDamage();

            double curFrac = (double) cur / (double) max;

            double newFrac = curFrac + (1.0 - curFrac) * p;

            int newDamage = (int) Math.floor(max * newFrac);
            if (newDamage >= max) newDamage = max - 1;

            dm.setDamage(newDamage);
            s.setItemMeta(dm);
            return s;
        }


        if (isStackable(s)) {
            int n = s.getAmount();
            double keepProb = 1.0 - p;
            int kept = 0;
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            for (int i = 0; i < n; i++) {
                if (rng.nextDouble() < keepProb) kept++;
            }
            if (kept <= 0) return null;
            s.setAmount(kept);
            return s;
        }

        // Non-damageable, non-stackable (maps, buckets, etc.)
        return ThreadLocalRandom.current().nextDouble() < p ? null : s;
    }

    /** Damageable = has durability meta. Do NOT exclude unbreakable here,
     *  otherwise they’d get deleted as “other”. If you want unbreakables immune,
     *  handle that in applyDecay by early-returning s.
     */
    public static boolean isDamageable(ItemStack s) {
        if (s == null || s.getType() == Material.AIR || s.getAmount() <= 0) return false;
        if (s.getType().getMaxDurability() <= 0) return false;
        ItemMeta meta = s.getItemMeta();
        return meta instanceof Damageable;
    }

    public static boolean isStackable(ItemStack s) {
        if (s == null || s.getType() == Material.AIR || s.getAmount() <= 0) return false;
        return s.getMaxStackSize() > 1;
    }

    /** Apply to a list, dropping nulls. */
    public static List<ItemStack> decayList(List<ItemStack> items, double p) {
        List<ItemStack> out = new ArrayList<>(items.size());
        for (ItemStack s : items) {
            ItemStack d = applyDecay(s, p);
            if (d != null) out.add(d);
        }
        return out;
    }
}
