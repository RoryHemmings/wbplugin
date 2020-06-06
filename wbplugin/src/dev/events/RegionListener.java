package dev.events;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import dev.ProtectedRegion;

public class RegionListener implements Listener {
	
	public static Map<Player, Location[]> playerRegions = new HashMap<Player, Location[]>();
	
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		@SuppressWarnings("deprecation")
		ItemStack heldItem = e.getPlayer().getItemInHand();
		Location loc = e.getBlock().getLocation();
		Player player = e.getPlayer();
		// add metadata to the stick in the future (and a command to get the stick)
		if (player.isOp() && heldItem != null && heldItem.getType() == Material.STICK) {
			player.sendMessage(String.format("pos1 (%.2f, %.2f, %.2f)", loc.getX(), loc.getY(), loc.getZ()));
			
			if (playerRegions.putIfAbsent(player, new Location[] {loc, null}) != null) {
				playerRegions.get(player)[0] = loc;
			}
			
			e.setCancelled(true);
			return;
		}
		
		if (!player.isOp() || player.getGameMode() != GameMode.CREATIVE) {
			for (ProtectedRegion region : ProtectedRegion.regions) { 
				if (region.contains(loc)) {
					if (region.isVerbose()) player.sendMessage("You can't change blocks in a protected region.");
					e.setCancelled(true);
					return;
				}
			}
		}
	}
	
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND) {
			@SuppressWarnings("deprecation")
			ItemStack heldItem = e.getPlayer().getItemInHand();
			Location loc = e.getPlayer().getTargetBlock(null, 5).getLocation();
			Player player = e.getPlayer();
			
			// add metadata to the stick in the future (and a command to get the stick)
			if (player.isOp() && heldItem != null && heldItem.getType() == Material.STICK) {
				e.getPlayer().sendMessage(String.format("pos2 (%.2f, %.2f, %.2f)", loc.getX(), loc.getY(), loc.getZ()));
				
				if (playerRegions.putIfAbsent(e.getPlayer(), new Location[] {null, loc}) != null) {
					playerRegions.get(e.getPlayer())[1] = loc;
				}
				
				e.setCancelled(true);
			}
			
			if (!player.isOp() || player.getGameMode() != GameMode.CREATIVE) {
				for (ProtectedRegion region : ProtectedRegion.regions) { 
					if (region.contains(loc)) {
						e.setCancelled(true);
						return;
					}
				}
			}
		}
	}
}