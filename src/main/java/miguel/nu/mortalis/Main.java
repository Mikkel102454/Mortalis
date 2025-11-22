package miguel.nu.mortalis;

import miguel.nu.mortalis.Commands.CommandListener;
import miguel.nu.mortalis.menus.GuiListener;
import miguel.nu.regula.users.BedrockJoinListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    public static Plugin plugin;

    public static PlayerDeath playerDeath;
    public static GraveHud graveHud;
    public static GraveProtection graveProtection;
    public static FileConfiguration config;
    @Override
    public void onEnable() {
        plugin = this;

        this.saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        new CommandListener(this);

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
