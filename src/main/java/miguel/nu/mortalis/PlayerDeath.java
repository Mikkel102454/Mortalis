package miguel.nu.mortalis;

import miguel.nu.mortalis.Classes.Gravestone;
import org.bukkit.*;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Material;

import java.util.*;

import static miguel.nu.mortalis.Decay.*;
import static miguel.nu.mortalis.Inventory.*;
import static net.kyori.adventure.util.Ticks.TICKS_PER_SECOND;

public class PlayerDeath implements Listener {
    public List<Gravestone> gravestones = new ArrayList<>();
    private BukkitRunnable timer;

    public final int safetyTime = Main.plugin.getConfig().getInt("gravestone.time.safety");
    public final int expireTime = Main.plugin.getConfig().getInt("gravestone.time.expire");

    public PlayerDeath(){
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
        gravestones = GravePersistent.loadGraves();
    }
    public void disable(){
        if (timer != null) {
            timer.cancel();
        }
        GravePersistent.saveGraves(gravestones);
    }

    public void registerDeath(Player player){
        Location freeLocation = GraveLocation.findNearestAirBlock(player.getLocation());
        Gravestone gravestone = new Gravestone();
        gravestone.setTimeLived(0);
        gravestone.setPlayer(player);
        gravestone.setLocation(freeLocation);

        gravestone.setItemStacks(player.getInventory().getStorageContents());
        gravestone.setHelmet(player.getInventory().getHelmet());
        gravestone.setChest(player.getInventory().getChestplate());
        gravestone.setLegs(player.getInventory().getLeggings());
        gravestone.setBoots(player.getInventory().getBoots());
        gravestone.setOffhand(player.getInventory().getItemInOffHand());

        gravestones.add(gravestone);
        Block block = freeLocation.getBlock();
        block.setType(Material.PLAYER_HEAD);
        GravePersistent.saveGraves(gravestones);

        if (block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.update(true);
        }
    }
    public void despawnGrave(Gravestone gravestone){
        gravestones.remove(gravestone);
        gravestone.getLocation().getBlock().setType(Material.AIR);
        GravePersistent.saveGraves(gravestones);
    }

    public boolean pickupGrave(Gravestone grave) {
        Player player = grave.getPlayer().getPlayer();
        if (player == null || !player.isOnline()) return false;

        final int time = grave.getTimeLived();
        final double p = decayFraction(time, safetyTime, expireTime); // 0..1

        PlayerInventory inv = player.getInventory();

        ItemStack helmet  = norm(grave.getHelmet());
        ItemStack chest   = norm(grave.getChest());
        ItemStack legs    = norm(grave.getLegs());
        ItemStack boots   = norm(grave.getBoots());
        ItemStack offhand = norm(grave.getOffhand());

        List<ItemStack> backpack = new ArrayList<>();
        for (ItemStack s : grave.getItemStacks()) {
            s = norm(s);
            if (s != null) backpack.add(s.clone());
        }

        List<ItemStack> displaced = new ArrayList<>();
        if (helmet  != null && isPresent(inv.getHelmet()))     displaced.add(inv.getHelmet().clone());
        if (chest   != null && isPresent(inv.getChestplate())) displaced.add(inv.getChestplate().clone());
        if (legs    != null && isPresent(inv.getLeggings()))   displaced.add(inv.getLeggings().clone());
        if (boots   != null && isPresent(inv.getBoots()))      displaced.add(inv.getBoots().clone());
        if (offhand != null && isPresent(inv.getItemInOffHand())) displaced.add(inv.getItemInOffHand().clone());

        ItemStack[] storageSnapshot = cloneArray(inv.getStorageContents());
        List<ItemStack> toStore = new ArrayList<>(displaced);
        toStore.addAll(backpack);
        if (!canFitAll(storageSnapshot, toStore)) {
            return false;
        }

        helmet  = applyDecay(helmet,  p);
        chest   = applyDecay(chest,   p);
        legs    = applyDecay(legs,    p);
        boots   = applyDecay(boots,   p);
        offhand = applyDecay(offhand, p);
        backpack = decayList(backpack, p);

        Map<Integer, ItemStack> leftover = inv.addItem(displaced.toArray(new ItemStack[0]));
        if (!leftover.isEmpty()) return false;

        if (helmet  != null) inv.setHelmet(helmet);
        if (chest   != null) inv.setChestplate(chest);
        if (legs    != null) inv.setLeggings(legs);
        if (boots   != null) inv.setBoots(boots);
        if (offhand != null) inv.setItemInOffHand(offhand);

        leftover = inv.addItem(backpack.toArray(new ItemStack[0]));
        if (!leftover.isEmpty()) return false;

        return true;
    }

    public void startGraveTimer() {
        timer = new BukkitRunnable() {
            private int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;

                List<Gravestone> toRemove = new ArrayList<>();

                for (Gravestone gravestone : gravestones) {
                    Main.plugin.getLogger().severe("Grave");
                    gravestone.setTimeLived(gravestone.getTimeLived() + 1);
                    if (gravestone.getTimeLived() > safetyTime + expireTime) {
                        toRemove.add(gravestone);
                    }
                }

                // remove expired graves
                for (Gravestone grave : toRemove) {
                    despawnGrave(grave);
                }

                if (tickCounter >= 30) {
                    tickCounter = 0;
                    GravePersistent.saveGraves(gravestones);
                }
            }
        };

        timer.runTaskTimer(Main.plugin, 0L, TICKS_PER_SECOND); // runs once per second
    }

    public Gravestone getGrave(Location location){
        for(Gravestone gravestone : gravestones){
            if(gravestone.getLocation().getBlock().getLocation()
                    .equals(location.getBlock().getLocation())){
                return gravestone;
            }
        }
        return null;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getPlayer();

        boolean hasItems = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) {
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    hasItems = true;
                    break;
                }
            }
        }
        if (!hasItems) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != null && offhand.getType() != Material.AIR) {
                hasItems = true;
            }
        }

        if (hasItems) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            registerDeath(player);
        }
    }

    private final Map<UUID, Long> pickupCooldown = new HashMap<>();
    private static final long PICKUP_COOLDOWN_MS = 300; // ~6 ticks

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
        // cooldown so we dont place item after grave pickup
        Long until = pickupCooldown.get(e.getPlayer().getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            e.setCancelled(true);
            e.setUseItemInHand(Event.Result.DENY);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock();
        if (clicked == null) return;
        if (gravestones.isEmpty()) return;

        Player player = e.getPlayer();

        Gravestone target = null;
        for (Gravestone g : gravestones) {
            if (!g.getPlayer().getUniqueId().equals(player.getUniqueId())) continue;
            if (g.getLocation().getBlock().equals(clicked)) { target = g; break; }
        }
        if (target == null) return;

        // cancel interaction for both hands to prevent placing/using items
        e.setCancelled(true);
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);

        if (!target.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§7This is not your grave!");
            return;
        }

        // only perform pickup once
        if (e.getHand() != EquipmentSlot.HAND) return;

        if (pickupGrave(target)) {
            gravestones.remove(target);

            pickupCooldown.put(player.getUniqueId(), System.currentTimeMillis() + PICKUP_COOLDOWN_MS);

            Bukkit.getScheduler().runTaskLater(Main.plugin, () ->
                    pickupCooldown.remove(player.getUniqueId()), 20L);

            clicked.setType(Material.AIR);
            GravePersistent.saveGraves(gravestones);
        } else {
            player.sendMessage("§7You need more inventory space to pick up your grave.");
        }
    }

}
