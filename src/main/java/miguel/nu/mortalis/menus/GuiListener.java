package miguel.nu.mortalis.menus;

import miguel.nu.regula.menus.MenuHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class GuiListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof MenuHolder holder) {
            switch (holder.getId()) {
                case "GRAVE_MENU" -> {
                    event.setCancelled(true);
                }
            }
        }
    }
}
