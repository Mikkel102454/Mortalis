package miguel.nu.mortalis;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

public interface GraveApi {
    void pickUpGrave(Location location);
    boolean isGrave(Location location);
    OfflinePlayer getGraveOwner(Location location);
}
