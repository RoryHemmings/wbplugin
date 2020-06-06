package dev.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.ProtectedRegion;
import dev.events.RegionListener;

public class ProtectedRegionCommand implements CommandExecutor {
	
	private void createSmartRegion(Player player) {
		Location[] locations = RegionListener.playerRegions.get(player);
		if (locations == null || locations[0] == null || locations[1] == null) {
			player.sendMessage("A selection must be made first");
		}
		else {
			
			
			player.sendMessage(String.format("Created a new smart protected region: (%.0f, %.0f, %.0f) (%.0f, %.0f, %.0f)", locations[0].getX(), locations[0].getY(), locations[0].getZ(), locations[1].getX(), locations[1].getY(), locations[1].getZ()));
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) return false;
		if (!(sender instanceof Player)) return true;
		Player player = (Player)sender;
		
		boolean smart = false, verbose = false;
		for (String arg : args) {
			if (arg.equals("-s")) smart = true;
			else if (arg.equals("-v")) verbose = true;
		}
		
		if (args[0].equalsIgnoreCase("pos1")) {
			Location loc = player.getLocation().getBlock().getLocation();
			player.sendMessage(String.format("pos1 (%.2f, %.2f, %.2f)", loc.getX(), loc.getY(), loc.getZ()));
			
			if (RegionListener.playerRegions.putIfAbsent(player, new Location[] {loc, null}) != null) {
				RegionListener.playerRegions.get(player)[0] = loc;
			}
		}
		else if (args[0].equalsIgnoreCase("pos2")) {
			Location loc = player.getLocation().getBlock().getLocation();
			player.sendMessage(String.format("pos2 (%.2f, %.2f, %.2f)", loc.getX(), loc.getY(), loc.getZ()));
			
			if (RegionListener.playerRegions.putIfAbsent(player, new Location[] {null, loc}) != null) {
				RegionListener.playerRegions.get(player)[1] = loc;
			}
		}
		else if (args[0].equalsIgnoreCase("create")) {
			Location[] locations = RegionListener.playerRegions.get(player);
			if (locations == null || locations[0] == null || locations[1] == null) {
				player.sendMessage("A selection must be made first");
			}
			else {
				ProtectedRegion.create(locations[0], locations[1], smart, verbose);
				player.sendMessage(String.format("Created a new protected region: (%.0f, %.0f, %.0f) (%.0f, %.0f, %.0f)", locations[0].getX(), locations[0].getY(), locations[0].getZ(), locations[1].getX(), locations[1].getY(), locations[1].getZ()));
			}
		}
		else if (args[0].equalsIgnoreCase("delete")) {
			Location[] locations = RegionListener.playerRegions.get(player);
			if (locations == null) {
				player.sendMessage("A selection must be made first");
			}
			else {
				for (ProtectedRegion region : ProtectedRegion.regions) {
					if (region.contains(locations[0]) || region.contains(locations[1])) {
						region.delete();
						ProtectedRegion.save();
						player.sendMessage("Deleted region");
						return true;
					}
				}
				player.sendMessage("No region found");
			}
		}
		else if (args[0].equalsIgnoreCase("deleteall")) {
			for (ProtectedRegion region : ProtectedRegion.regions) {
				region.delete();
			}
			ProtectedRegion.save();
			player.sendMessage("Deleted all protected regions");
		}
		else if (args[0].equalsIgnoreCase("list")) {
			int num = 1;
			for (ProtectedRegion region : ProtectedRegion.regions) {
				player.sendMessage(String.format("%d. %s", num++, region.toString()));
			}
			if (num == 1) player.sendMessage("No regions found");
		}
		else if (args[0].equalsIgnoreCase("test")) {
			Location[] locations = RegionListener.playerRegions.get(player);
			if (locations == null || locations[0] == null) {
				player.sendMessage("A selection must be made first");
			}
			else {
				player.sendMessage(String.format("Testing for region at pos1(%.0f, %.0f, %.0f)", locations[0].getX(), locations[0].getY(), locations[0].getZ()));
				int numRegions = 0;
				for (ProtectedRegion region : ProtectedRegion.regions) {
					if (region.contains(locations[0]))
						++numRegions;
				}
				if (numRegions != 0) player.sendMessage(String.format("Found %d regions", numRegions));
				else player.sendMessage("No region found");
			}
		}
		else return false;
		
		return true;
	}

}