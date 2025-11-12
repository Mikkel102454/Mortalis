package miguel.nu.mortalis;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

public class GraveApiImpl implements GraveApi{
    @Override
    public void pickUpGrave(Location location) {
        Main.playerDeath.despawnGrave(Main.playerDeath.getGrave(location));
    }

    @Override
    public boolean isGrave(Location location) {
        return Main.graveProtection.isGraveBlock(location.getBlock());
    }
    @Override
    public OfflinePlayer getGraveOwner(Location location) {
        return Main.playerDeath.getGrave(location).getPlayer();
    }
}
