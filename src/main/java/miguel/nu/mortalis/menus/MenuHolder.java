package miguel.nu.mortalis.menus;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuHolder implements InventoryHolder {
    private final Inventory inv;
    private final String id;

    public MenuHolder(String id, int size, Component title) {
        this.id = id;
        this.inv = Bukkit.createInventory(this, size, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public String getId() {
        return id;
    }
}
