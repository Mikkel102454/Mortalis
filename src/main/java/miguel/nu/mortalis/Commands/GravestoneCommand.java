package miguel.nu.mortalis.Commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import miguel.nu.mortalis.Classes.Gravestone;
import miguel.nu.mortalis.Main;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class GravestoneCommand implements BasicCommand {
    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if(!(source.getSender() instanceof Player)){
            source.getSender().sendMessage(Main.config.getString("message.not-player"));
            return;
        }
        Player player = (Player) source.getSender();

        if(args.length == 0){
            Main.plugin.reloadConfig();
            List<Gravestone> gravestones = Main.playerDeath.gravestones;

            boolean gaveMessage = false;
            for(Gravestone gravestone : gravestones){
                if(!Objects.equals(gravestone.getPlayer().getUniqueId().toString(), player.getUniqueId().toString())) continue;
                String message = Main.config.getString("message.gravestone-location");
                message = message.replace("%coord_x%", String.valueOf(gravestone.getLocation().getX()));
                message = message.replace("%coord_y%", String.valueOf(gravestone.getLocation().getY()));
                message = message.replace("%coord_z%", String.valueOf(gravestone.getLocation().getZ()));
                source.getSender().sendMessage(message);
                gaveMessage = true;
            }

            if(!gaveMessage){
                source.getSender().sendMessage(Main.config.getString("message.no-gravestone"));
            }
            return;
        } else if(player.isOp() && Objects.equals(args[0], "reload")){
            Main.plugin.reloadConfig();
            FileConfiguration config = Main.plugin.getConfig();
            Main.config = config;
            source.getSender().sendMessage("§aConfig for gravestone has been reloaded");
            return;
        }

        source.getSender().sendMessage(Main.config.getString("message.unknown-command"));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (!source.getSender().isOp()) return List.of();

        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            if ("reload".startsWith(typed)) {
                return List.of("reload");
            }
        }
        return List.of();
    }
}
