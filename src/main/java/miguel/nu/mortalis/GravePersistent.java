package miguel.nu.mortalis;


import miguel.nu.mortalis.Classes.Gravestone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GravePersistent {
    public static List<Gravestone> loadGraves() {
        File file = new File(Main.plugin.getDataFolder(), "gravestones.yml");
        List<Gravestone> result = new ArrayList<>();
        if (!file.exists()) return result;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("graves");
        if (root == null) return result;

        for (String key : root.getKeys(false)) {
            String base = "graves." + key;

            // owner
            String ownerStr = cfg.getString(base + ".player");
            if (ownerStr == null) continue;
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerStr));

            // world and location
            World world = null;
            String worldUID = cfg.getString(base + ".world");
            if (worldUID != null) {
                world = Bukkit.getWorld(UUID.fromString(worldUID));
            }
            if (world == null) {
                String worldName = cfg.getString(base + ".worldName"); // fallback by name
                if (worldName != null) world = Bukkit.getWorld(worldName);
            }
            if (world == null) continue; // can't place without a world

            double x = cfg.getDouble(base + ".x");
            double y = cfg.getDouble(base + ".y");
            double z = cfg.getDouble(base + ".z");
            Location loc = new Location(world, x, y, z);

            // time
            int time = cfg.getInt(base + ".timeLived", 0);

            // armor/offhand
            ItemStack helmet  = cfg.getItemStack(base + ".helmet");
            ItemStack chest   = cfg.getItemStack(base + ".chest");
            ItemStack legs    = cfg.getItemStack(base + ".legs");
            ItemStack boots   = cfg.getItemStack(base + ".boots");
            ItemStack offhand = cfg.getItemStack(base + ".offhand");

            // inventory (36 storage slots)
            // Bukkit can deserialize ItemStack[] directly
            ItemStack[] inv = null;
            Object raw = cfg.get(base + ".inventory");
            if (raw instanceof ItemStack[]) {
                inv = (ItemStack[]) raw;
            } else {
                @SuppressWarnings("unchecked")
                List<ItemStack> list = (List<ItemStack>) cfg.getList(base + ".inventory");
                if (list != null) inv = list.toArray(new ItemStack[0]);
            }
            if (inv == null) inv = new ItemStack[36];

            Gravestone g = new Gravestone();
            g.setTimeLived(time);
            g.setPlayer(owner);
            g.setLocation(loc);
            g.setItemStacks(inv);
            g.setHelmet(helmet);
            g.setChest(chest);
            g.setLegs(legs);
            g.setBoots(boots);
            g.setOffhand(offhand);

            result.add(g);
        }
        return result;
    }

    public static void saveGraves(List<Gravestone> graves) {
        File dir = Main.plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "gravestones.yml");

        YamlConfiguration cfg = new YamlConfiguration();
        int i = 0;
        for (Gravestone g : graves) {
            String base = "graves." + (i++);

            // owner
            OfflinePlayer owner = g.getPlayer();
            if (owner != null && owner.getUniqueId() != null) {
                cfg.set(base + ".player", owner.getUniqueId().toString());
            }

            // world and location
            Location loc = g.getLocation();
            if (loc != null && loc.getWorld() != null) {
                cfg.set(base + ".world", loc.getWorld().getUID().toString());
                cfg.set(base + ".worldName", loc.getWorld().getName()); // fallback
                cfg.set(base + ".x", loc.getX());
                cfg.set(base + ".y", loc.getY());
                cfg.set(base + ".z", loc.getZ());
            }

            // time
            cfg.set(base + ".timeLived", g.getTimeLived());

            // armor/offhand
            cfg.set(base + ".helmet", g.getHelmet());
            cfg.set(base + ".chest", g.getChest());
            cfg.set(base + ".legs", g.getLegs());
            cfg.set(base + ".boots", g.getBoots());
            cfg.set(base + ".offhand", g.getOffhand());

            // inventory (ItemStack[] is ConfigurationSerializable)
            ItemStack[] inv = g.getItemStacks();
            cfg.set(base + ".inventory", inv == null ? new ItemStack[36] : inv);
        }

        try {
            cfg.save(file);
        } catch (IOException ex) {
            Main.plugin.getLogger().severe("Failed to save gravestones.yml: " + ex.getMessage());
        }
    }
}
