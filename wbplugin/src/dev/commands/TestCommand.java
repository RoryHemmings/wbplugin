package dev.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.MiniGame;

public class TestCommand implements CommandExecutor {
	
	private MiniGame game;
	
	public TestCommand(MiniGame game) {
		this.game = game;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		
		Player player = (Player) sender;
		player.sendMessage("Test Succesful");
		
		return true;
	}
	
}
