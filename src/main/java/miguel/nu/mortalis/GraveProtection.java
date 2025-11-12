package miguel.nu.mortalis;


import miguel.nu.mortalis.Classes.Gravestone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;

public class GraveProtection implements Listener{

    PlayerDeath playerDeath;

    public GraveProtection(PlayerDeath playerDeath) {
        this.playerDeath = playerDeath;
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
    }

    private boolean isGraveBlock(Block block) {
        if (playerDeath.gravestones.isEmpty()) return false;
        for (Gravestone g : playerDeath.gravestones) {
            if (g.getLocation().getBlock().equals(block)) return true;
        }
        return false;
    }

    // Direct breaking (player or otherwise)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (isGraveBlock(e.getBlock())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§7You cannot break a grave.");
        }
    }

    // Pistons pushing OUT
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (isGraveBlock(b)) {
                e.setCancelled(true);
                return;
            }
        }
        // Also protect destination block (where things will be pushed into)
        Block dest = e.getBlock().getRelative(e.getDirection());
        for (Block b : e.getBlocks()) dest = dest.getRelative(e.getDirection());
        if (isGraveBlock(dest)) e.setCancelled(true);
    }

    // Pistons pulling IN (sticky retract)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (isGraveBlock(b)) {
                e.setCancelled(true);
                return;
            }
        }
        // block where it would be pulled into
        Block dest = e.getBlock().getRelative(e.getDirection().getOppositeFace());
        if (isGraveBlock(dest)) e.setCancelled(true);
    }

    // Explosions (TNT, creeper, wither, crystals, etc.)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        // remove grave blocks from the list of blocks to be destroyed
        Iterator<Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            if (isGraveBlock(it.next())) it.remove();
        }
    }

    // Block explosions (e.g., bed in nether/end)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        Iterator<Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            if (isGraveBlock(it.next())) it.remove();
        }
    }

    // Fluid flow replacing the block (water/lava)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent e) {
        if (isGraveBlock(e.getToBlock())) {
            e.setCancelled(true);
        }
    }

    //Physics/entity edits (endermen, wither, falling blocks, ravager, etc.)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (isGraveBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    // Fire / burning
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (isGraveBlock(e.getBlock())) e.setCancelled(true);
    }

    // Fade / melt / grow physics that could replace heads (ice melt, etc.)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        if (isGraveBlock(e.getBlock())) e.setCancelled(true);
    }
}
