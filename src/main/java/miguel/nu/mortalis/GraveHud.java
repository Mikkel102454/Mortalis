package miguel.nu.mortalis;

import miguel.nu.mortalis.Classes.Gravestone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class GraveHud {
    private static final int HUD_TICKS = 10;
    private static final int RAY_RANGE = 6;

    PlayerDeath playerDeath;
    public GraveHud(PlayerDeath playerDeath) {
        this.playerDeath = playerDeath;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(Main.plugin, () -> {

            if (playerDeath.gravestones.isEmpty()) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                Block target = p.getTargetBlockExact(RAY_RANGE);
                if (target == null) {
                    continue;
                }

                Gravestone g = findGraveByBlock(target);
                if (g == null) {
                    continue;
                }

                int time = g.getTimeLived();
                int safe = playerDeath.safetyTime;
                int dur = playerDeath.expireTime;
                int end = safe + dur;

                if (!g.getPlayer().getUniqueId().equals(p.getUniqueId())) {
                    int despawnLeft = Math.max(0, end - time);
                    p.sendActionBar(Component.text("Not your grave • Despawns in " + Utils.formatDuration(despawnLeft))
                            .color(NamedTextColor.GRAY));
                    continue;
                }

                if (time < safe) {
                    int safeLeft = safe - time;
                    p.sendActionBar(Component.text("Safe: " + Utils.formatDuration(safeLeft) + " left")
                            .color(NamedTextColor.GREEN));
                } else {
                    double decay = Decay.decayFraction(time, safe, dur);
                    int percent = (int) Math.round(decay * 100);
                    int despawnLeft = Math.max(0, end - time);

                    p.sendActionBar(Component.text()
                            .append(Component.text("Decay: ", NamedTextColor.GRAY))
                            .append(Component.text(percent + "%", NamedTextColor.YELLOW))
                            .append(Component.text("  •  Despawn in ", NamedTextColor.GRAY))
                            .append(Component.text(Utils.formatDuration(despawnLeft), NamedTextColor.RED))
                            .build());
                }
            }
        }, 0L, HUD_TICKS);
    }

    private Gravestone findGraveByBlock(Block block) {
        Location loc = block.getLocation();
        for (Gravestone g : playerDeath.gravestones) {
            if (g.getLocation().getBlock().equals(block)) return g;
        }
        return null;
    }
}