package miguel.nu.mortalis;

import com.google.gson.*;
import miguel.nu.mortalis.Classes.Gravestone;
import miguel.nu.mortalis.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GravePersistent {

    // ---------------------------------------------------------------------
    // File helpers
    // ---------------------------------------------------------------------

    private static File getGravesFile() {
        return new File(Main.plugin.getDataFolder(), "gravestones.yml");
    }

    private static File getGravesArchiveFile() {
        return new File(Main.plugin.getDataFolder(), "gravestones_longterm.json");
    }

    // ---------------------------------------------------------------------
    // SHORT-TERM: load/save all current graves (startup, normal operation)
    // ---------------------------------------------------------------------

    /**
     * Load all "active" graves from gravestones.yml (short-term).
     */
    public static List<Gravestone> loadGraves() {
        File file = getGravesFile();
        List<Gravestone> result = new ArrayList<>();
        if (!file.exists()) return result;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("graves");
        if (root == null) return result;

        for (String key : root.getKeys(false)) {
            String base = "graves." + key;

            Gravestone g = deserializeGrave(cfg, base);
            if (g == null) continue;

            result.add(g);
        }
        return result;
    }

    /**
     * Save all active graves to gravestones.yml (short-term).
     * This overwrites the file each time.
     */
    public static void saveGraves(List<Gravestone> graves) {
        File dir = Main.plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();

        File file = getGravesFile();
        YamlConfiguration cfg = new YamlConfiguration();

        int i = 0;
        for (Gravestone g : graves) {
            String base = "graves." + (i++);
            serializeGrave(cfg, base, g);
        }

        try {
            if(Main.config.getBoolean("gravestone.debug")){
                Main.plugin.getLogger().info("\n====== Saved gravestones.yml ======\n"
                        + cfg.saveToString() +
                        "\n===================================");

            }

            cfg.save(file);
        } catch (IOException ex) {
            Main.plugin.getLogger().severe("Failed to save gravestones.yml: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // LONG-TERM: archive save + load by UUID
    // ---------------------------------------------------------------------
    private static final Gson GSON = new GsonBuilder().create();
    /**
     * Save or update a single grave in the long-term archive under a UUID.
     * This file is never wiped; entries are stored as graves.<uuid>.
     */
    public static void saveGraveLongTerm(UUID graveId, Gravestone grave) {
        if (graveId == null || grave == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, () -> {
            File dir = Main.plugin.getDataFolder();
            if (!dir.exists()) dir.mkdirs();

            File archiveFile = getGravesArchiveFile();

            JsonObject obj = new JsonObject();
            obj.addProperty("graveId", graveId.toString());

            obj.addProperty("player", grave.getPlayer().toString());
            obj.addProperty("world", grave.getLocation().getWorld().getUID().toString());
            obj.addProperty("worldName", grave.getLocation().getWorld().getName());
            obj.addProperty("x", grave.getLocation().x());
            obj.addProperty("y", grave.getLocation().y());
            obj.addProperty("z", grave.getLocation().z());
            obj.addProperty("helmet", grave.getHelmet().toString());
            obj.addProperty("chest", grave.getChest().toString());
            obj.addProperty("legs", grave.getLegs().toString());
            obj.addProperty("boots", grave.getBoots().toString());
            obj.addProperty("offhand", grave.getOffhand().toString());

            JsonArray inv = new JsonArray();
            ItemStack[] items = grave.getInventory();
            for (ItemStack item : items){
                if(item != null){
                    inv.add(item.toString());
                }
            }

            obj.add("inventory", inv);
            String line = GSON.toJson(obj);

            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(archiveFile, StandardCharsets.UTF_8, true))) {

                writer.write(line);
                writer.newLine();
            } catch (IOException ex) {
                Main.plugin.getLogger().severe("Failed to append grave to gravestones_longterm.jsonl: " + ex.getMessage());
            }
        });
    }

    // ---------------------------------------------------------------------
    // Shared (de)serialization helpers – based on your original code
    // ---------------------------------------------------------------------

    private static void serializeGrave(YamlConfiguration cfg, String base, Gravestone g) {
        // owner
        OfflinePlayer owner = Bukkit.getOfflinePlayer(g.getPlayer());
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

    @SuppressWarnings("unchecked")
    private static Gravestone deserializeGrave(YamlConfiguration cfg, String base) {
        // owner
        String ownerStr = cfg.getString(base + ".player");
        if (ownerStr == null) return null;

        OfflinePlayer owner;
        try {
            owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerStr));
        } catch (IllegalArgumentException ex) {
            return null;
        }

        // world and location
        World world = null;
        String worldUID = cfg.getString(base + ".world");
        if (worldUID != null) {
            try {
                world = Bukkit.getWorld(UUID.fromString(worldUID));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (world == null) {
            String worldName = cfg.getString(base + ".worldName"); // fallback by name
            if (worldName != null) world = Bukkit.getWorld(worldName);
        }
        if (world == null) return null; // can't place without a world

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
        ItemStack[] inv = null;
        Object raw = cfg.get(base + ".inventory");
        if (raw instanceof ItemStack[]) {
            inv = (ItemStack[]) raw;
        } else {
            List<ItemStack> list = (List<ItemStack>) cfg.getList(base + ".inventory");
            if (list != null) inv = list.toArray(new ItemStack[0]);
        }
        if (inv == null) inv = new ItemStack[36];

        Gravestone g = new Gravestone();
        g.setTimeLived(time);
        g.setPlayer(owner.getUniqueId());
        g.setLocation(loc);
        g.setItemStacks(inv);
        g.setHelmet(helmet);
        g.setChest(chest);
        g.setLegs(legs);
        g.setBoots(boots);
        g.setOffhand(offhand);

        return g;
    }
}
