package miguel.nu.mortalis.menus;

import miguel.nu.mortalis.Classes.Gravestone;
import miguel.nu.regula.menus.MenuHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GraveMenu {
    public static void open(Player player, Gravestone grave){
        MenuHolder holder = new MenuHolder("GRAVE_MENU", 45, Component.text(grave.getPlayer().getName() + "'s grave (READ ONLY)"));
        Inventory inventory = holder.getInventory();

        ItemStack[] items = grave.getItemStacks();
        for(int i = 0; i < items.length; i++){
            inventory.setItem(i, items[i]);
        }

        ItemStack helmet = grave.getHelmet();
        ItemStack chest = grave.getChest();
        ItemStack legs = grave.getLegs();
        ItemStack boots = grave.getBoots();
        ItemStack offhand = grave.getOffhand();

        ItemStack noItem = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = noItem.getItemMeta();
        meta.setHideTooltip(true);
        noItem.setItemMeta(meta);

        if(helmet != null && helmet.getType() != Material.AIR){
            inventory.setItem(36, helmet);
        }else {
            inventory.setItem(36, noItem);
        }

        if(chest != null && chest.getType() != Material.AIR){
            inventory.setItem(37, chest);
        }else {
            inventory.setItem(37, noItem);
        }

        if(legs != null && legs.getType() != Material.AIR){
            inventory.setItem(38, legs);
        }else {
            inventory.setItem(38, noItem);
        }

        if(boots != null && boots.getType() != Material.AIR){
            inventory.setItem(39, boots);
        }else {
            inventory.setItem(39, noItem);
        }

        if(offhand != null && offhand.getType() != Material.AIR){
            inventory.setItem(40, offhand);
        }else {
            inventory.setItem(40, noItem);
        }

        inventory.setItem(41, noItem);
        inventory.setItem(42, noItem);
        inventory.setItem(43, noItem);
        inventory.setItem(44, noItem);

        player.openInventory(inventory);
    }

}
