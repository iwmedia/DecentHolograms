package eu.decentsoftware.holograms.api.listeners;

import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.Lang;
import eu.decentsoftware.holograms.api.Settings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final DecentHolograms decentHolograms;

    public PlayerListener(DecentHolograms decentHolograms) {
        this.decentHolograms = decentHolograms;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        this.decentHolograms.getScheduler().runAsync(() -> decentHolograms.getHologramManager().updateVisibility(player));
        if (decentHolograms.isUpdateAvailable() && player.hasPermission("dh.admin")) {
            Lang.sendUpdateMessage(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        this.decentHolograms.getScheduler().runAsync(() -> decentHolograms.getHologramManager().onQuit(player));
    }

    // TODO: All holograms (and entities) get hidden on the client, when the client
    //  teleports or respawns. This only seems to be happening on some client versions
    //  so we need to find which versions are affected and only re-show the holograms
    //  to those clients (or on those server versions).
    //  -
    //  For now, this only causes visual glitches where even if a player gets teleported
    //  by a fraction of a block, the holograms still disappear and reappear for them.
    //  -
    //  tl:dr Figure out which versions need this.

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (Settings.UPDATE_VISIBILITY_ON_TELEPORT) {
            this.decentHolograms.getScheduler().runAsync(() -> decentHolograms.getHologramManager().hideAll(e.getPlayer()));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (Settings.UPDATE_VISIBILITY_ON_TELEPORT) {
            this.decentHolograms.getScheduler().runAsync(() -> decentHolograms.getHologramManager().hideAll(e.getPlayer()));
        }
    }

}
