package miguel.nu.mortalis;

import miguel.nu.mortalis.Classes.Gravestone;
import miguel.nu.mortalis.menus.GraveMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Skull;
import org.bukkit.entity.Item;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Material;
import miguel.nu.regula.API.RoleAPI;
import miguel.nu.discordRelay.API.DiscordAPI;
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
        Location freeLocation = GraveLocation.findGraveLocation(player.getLocation());
        Gravestone gravestone = new Gravestone();
        gravestone.setTimeLived(0);
        gravestone.setPlayer(player);
        gravestone.setLocation(freeLocation);

        PlayerInventory inv = player.getInventory();
        gravestone.setItemStacks(inv.getStorageContents());
        gravestone.setOffhand(inv.getItemInOffHand());
        gravestone.setHelmet(inv.getHelmet());
        gravestone.setChest(inv.getChestplate());
        gravestone.setLegs(inv.getLeggings());
        gravestone.setBoots(inv.getBoots());

        gravestones.add(gravestone);
        Block block = freeLocation.getBlock();
        block.setType(Material.PLAYER_HEAD);
        GravePersistent.saveGraves(gravestones);
        GravePersistent.saveGraveLongTerm(UUID.randomUUID(), gravestone);

        if (block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.update(true);
        }

        player.sendMessage("§cYour gravestone spawned at (X: "
                + freeLocation.x() + ", Y:"
                + freeLocation.y() + ", Z:"
                + freeLocation.z() + ")");
    }
    public void despawnGrave(Gravestone gravestone){
        gravestones.remove(gravestone);
        gravestone.getLocation().getBlock().setType(Material.AIR);
        GravePersistent.saveGraves(gravestones);
    }

    public int pickupGrave(Gravestone grave) {
        boolean debug = Main.config.getBoolean("gravestone.debug");
        if(debug) Main.plugin.getLogger().info("Getting player for gravestone");

        Player player = grave.getPlayer().getPlayer();
        if (player == null || !player.isOnline()) {
            if(debug) Main.plugin.getLogger().warning("Could not get the player. Returning!");
            return 1;
        }

        if(debug) Main.plugin.getLogger().info("Got the player named " + player.getName());

        final int time = grave.getTimeLived();
        final double p = decayFraction(time, safetyTime, expireTime);

        if(debug){
            Main.plugin.getLogger().info("The grave has been alive for " + time);
            Main.plugin.getLogger().info("The grave have decayed " + p + "%");
        }

        PlayerInventory inv = player.getInventory();

        List<ItemStack> inventoryItems = new ArrayList<>();
        inventoryItems.addAll(Arrays.asList(inv.getStorageContents()));
        inventoryItems.add(inv.getItemInOffHand());
        inventoryItems.addAll(Arrays.asList(inv.getArmorContents()));

        if(debug){
            int invSpace = 0;
            for (ItemStack item : inventoryItems){
                if(item == null || item.getType() == Material.AIR) invSpace++;
            }

            Main.plugin.getLogger().info("The player has " + invSpace + " free spaces in inventory");
        }

        if(debug){
            Main.plugin.getLogger().info("Getting items out of grave");
        }

        ItemStack[] items = grave.getItemStacks();
        List<ItemStack> backpack = new ArrayList<>();

        for (ItemStack stack : items) {
            if (isPresent(stack)) {
                backpack.add(stack.clone());
            }
        }

        backpack.add(grave.getOffhand());
        backpack.add(grave.getBoots());
        backpack.add(grave.getLegs());
        backpack.add(grave.getChest());
        backpack.add(grave.getHelmet());
        if(debug){
            int invSpace = 0;
            for (ItemStack item : backpack){
                if(item != null && item.getType() != Material.AIR) invSpace++;
            }

            Main.plugin.getLogger().info("There was a total of " + invSpace + " items in the grave");
            Main.plugin.getLogger().info("There was a total of " + backpack.size() + " slots in the grave");
        }

        if(!canFitAll(inventoryItems, backpack, debug)){
            return 2;
        }

        if(debug){
            Main.plugin.getLogger().info("Applying decay to items");
        }

        ItemStack helmet = applyDecay(backpack.getLast(), p);
        ItemStack chest = applyDecay(backpack.get(backpack.size()-2), p);
        ItemStack legs = applyDecay(backpack.get(backpack.size()-3), p);
        ItemStack boots = applyDecay(backpack.get(backpack.size()-4), p);
        ItemStack offhand = applyDecay(backpack.get(backpack.size()-5), p);
        backpack = decayList(backpack, p);

        if(debug){
            Main.plugin.getLogger().info("Giving armor + offhand.");
        }

        if (isEmpty(inv.getHelmet()) && isPresent(helmet)) {
            inv.setHelmet(helmet);
            backpack.remove(helmet);
        }

        if (isEmpty(inv.getChestplate()) && isPresent(chest)) {
            inv.setChestplate(chest);
            backpack.remove(chest);
        }

        if (isEmpty(inv.getLeggings()) && isPresent(legs)) {
            inv.setLeggings(legs);
            backpack.remove(legs);
        }

        if (isEmpty(inv.getBoots()) && isPresent(boots)) {
            inv.setBoots(boots);
            backpack.remove(boots);
        }

        if (isEmpty(inv.getItemInOffHand()) && isPresent(offhand)){
            inv.setItemInOffHand(offhand);
            backpack.remove(offhand);
        }

        if (backpack.size() > 36) {
            backpack = backpack.subList(0, 36);
        }
        ItemStack[] backpackArray = backpack.toArray(new ItemStack[0]);
        if(debug){
            Main.plugin.getLogger().info("giving items. Items to give is " + backpackArray.length);
        }


        Map<Integer, ItemStack> leftover = inv.addItem(backpackArray);
        if (!leftover.isEmpty()) {
            Main.plugin.getLogger().warning("For some reason the player did not have enough space even tho we got earlier that they have!");
            return 3;
        }

        if(debug) {
            Main.plugin.getLogger().info("all items added to players inventory");
        }
        return 0;
    }

    public void startGraveTimer() {
        timer = new BukkitRunnable() {
            private int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;

                List<Gravestone> toRemove = new ArrayList<>();

                for (Gravestone gravestone : gravestones) {
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
        Component component = e.deathMessage();
        String deathMessage = "";
        if(component != null){
            deathMessage = PlainTextComponentSerializer.plainText().serialize(component);
        }
        DiscordAPI.sendDeathLog(e.getPlayer(), deathMessage, e.getPlayer().getLocation());
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
            if (g.getLocation().getBlock().equals(clicked)) { target = g; break; }
        }
        if (target == null) return;

        // cancel interaction for both hands to prevent placing/using items
        e.setCancelled(true);
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);

        // only perform pickup once
        if (e.getHand() != EquipmentSlot.HAND) return;

        if (!target.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            if(RoleAPI.hasPlayerPermission(player,
                            Main.config.getString("permission.inspect"))){
                GraveMenu.open(player, target);
            }else {
                player.sendMessage("§7This is not your grave!");
            }
            return;
        }

        int exitCode = pickupGrave(target);
        if (exitCode == 0) {
            gravestones.remove(target);

            pickupCooldown.put(player.getUniqueId(), System.currentTimeMillis() + PICKUP_COOLDOWN_MS);

            Bukkit.getScheduler().runTaskLater(Main.plugin, () ->
                    pickupCooldown.remove(player.getUniqueId()), 20L);

            clicked.setType(Material.AIR);
            GravePersistent.saveGraves(gravestones);
        } else if (exitCode == 2) {
            player.sendMessage("§7You need more inventory space to pick up your grave.");
        } else {
            player.sendMessage("§7Something went wrong while trying to pickup your grave. Please contact the developer!");
            Main.plugin.getLogger().severe("Someone tried to pickup a grave and failed. Exit code: " + exitCode);
        }
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0;
    }

    private static boolean isPresent(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && stack.getAmount() > 0;
    }
}
