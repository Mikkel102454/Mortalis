package miguel.nu.mortalis;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    public static Plugin plugin;

    public static PlayerDeath playerDeath;
    public static GraveHud graveHud;
    public static GraveProtection graveProtection;
    @Override
    public void onEnable() {
        plugin = this;

        this.saveDefaultConfig();
        playerDeath = new PlayerDeath();
        playerDeath.startGraveTimer();
        graveProtection = new GraveProtection(playerDeath);

        graveHud = new GraveHud(playerDeath);
        graveHud.start();
    }

    @Override
    public void onDisable() {
        playerDeath.disable();
    }
}
