package miguel.nu.mortalis.Classes;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Gravestone {
    int timeLived;
    ItemStack[] inventory;
    Location location;
    UUID playerUuid;
    ItemStack helmet;
    ItemStack chest;
    ItemStack legs;
    ItemStack boots;
    ItemStack offhand;

    public int getTimeLived() {
        return timeLived;
    }

    public void setTimeLived(int timeLived) {
        this.timeLived = timeLived;
    }

    public ItemStack[] getItemStacks() {
        return inventory;
    }

    public void setItemStacks(ItemStack[] itemStacks) {
        this.inventory = itemStacks;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public UUID getPlayer() {
        return playerUuid;
    }

    public void setPlayer(UUID player) {
        this.playerUuid = player;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChest() {
        return chest;
    }

    public void setChest(ItemStack chest) {
        this.chest = chest;
    }

    public ItemStack getLegs() {
        return legs;
    }

    public void setLegs(ItemStack legs) {
        this.legs = legs;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    public ItemStack getOffhand() {
        return offhand;
    }

    public void setOffhand(ItemStack offhand) {
        this.offhand = offhand;
    }
}
