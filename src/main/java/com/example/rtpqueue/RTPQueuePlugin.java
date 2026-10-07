package com.example.rtpqueue;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RTPQueuePlugin extends JavaPlugin implements CommandExecutor {

    private Player queuedPlayer = null;
    private final Map<UUID, UUID> pendingChallenges = new HashMap<>();
    private final List<String> targetWorlds = Arrays.asList("world", "world_nether", "world_the_end");
    private final Random random = new Random();

    @Override
    public void onEnable() {
        if (getCommand("rtpqueue") != null) getCommand("rtpqueue").setExecutor(this);
        if (getCommand("thachdau") != null) getCommand("thachdau").setExecutor(this);
        getLogger().info("RTPQueue Duel Plugin enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Chỉ người chơi mới có thể dùng lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("rtpqueue")) {
            handleQueue(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("thachdau")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.YELLOW + "Cú pháp: /thachdau <tên_người_chơi> hoặc /thachdau accept");
                return true;
            }

            if (args[0].equalsIgnoreCase("accept")) {
                handleAccept(player);
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Người chơi không online!");
                return true;
            }

            if (target.equals(player)) {
                player.sendMessage(ChatColor.RED + "Bạn không thể tự thách đấu chính mình!");
                return true;
            }

            pendingChallenges.put(target.getUniqueId(), player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Đã gửi lời thách đấu đến " + target.getName() + "!");
            target.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " đã thách đấu bạn! Nhập " + ChatColor.GREEN + "/thachdau accept" + ChatColor.YELLOW + " để đồng ý.");
            return true;
        }

        return false;
    }

    private void handleQueue(Player player) {
        if (queuedPlayer == null) {
            queuedPlayer = player;
            player.sendMessage(ChatColor.GREEN + "Đã vào hàng đợi RTP Thách đấu! Đang chờ đối thủ...");
        } else if (queuedPlayer.equals(player)) {
            player.sendMessage(ChatColor.YELLOW + "Bạn đang ở trong hàng đợi rồi.");
        } else if (!queuedPlayer.isOnline()) {
            queuedPlayer = player;
            player.sendMessage(ChatColor.GREEN + "Đã vào hàng đợi RTP Thách đấu! Đang chờ đối thủ...");
        } else {
            Player opponent = queuedPlayer;
            queuedPlayer = null;

            player.sendMessage(ChatColor.GOLD + "Đã tìm thấy đối thủ: " + opponent.getName() + "! Đang tìm vị trí teleport...");
            opponent.sendMessage(ChatColor.GOLD + "Đã tìm thấy đối thủ: " + player.getName() + "! Đang tìm vị trí teleport...");

            startDuelRTP(player, opponent);
        }
    }

    private void handleAccept(Player target) {
        UUID challengerId = pendingChallenges.remove(target.getUniqueId());
        if (challengerId == null) {
            target.sendMessage(ChatColor.RED + "Bạn không có lời thách đấu nào đang chờ!");
            return;
        }

        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null || !challenger.isOnline()) {
            target.sendMessage(ChatColor.RED + "Người thách đấu đã ngắt kết nối!");
            return;
        }

        target.sendMessage(ChatColor.GREEN + "Đã chấp nhận lời thách đấu từ " + challenger.getName() + "!");
        challenger.sendMessage(ChatColor.GREEN + target.getName() + " đã chấp nhận lời thách đấu!");

        startDuelRTP(challenger, target);
    }

    private void startDuelRTP(Player p1, Player p2) {
        String worldName = targetWorlds.get(random.nextInt(targetWorlds.size()));
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            p1.sendMessage(ChatColor.RED + "Lỗi: Không tìm thấy thế giới " + worldName);
            p2.sendMessage(ChatColor.RED + "Lỗi: Không tìm thấy thế giới " + worldName);
            return;
        }

        Location spawnLoc = world.getSpawnLocation();
        Location duelLoc = findSafeLocation(world, spawnLoc, 500);

        if (duelLoc == null) {
            p1.sendMessage(ChatColor.RED + "Không tìm được vị trí an toàn, vui lòng thử lại!");
            p2.sendMessage(ChatColor.RED + "Không tìm được vị trí an toàn, vui lòng thử lại!");
            return;
        }

        Location p1Loc = duelLoc.clone().add(1, 0, 0);
        Location p2Loc = duelLoc.clone().add(-1, 0, 0);

        p1.teleport(p1Loc);
        p2.teleport(p2Loc);

        String msg = ChatColor.GOLD + "========== PHÒNG THÁCH ĐẤU ==========\n" +
                     ChatColor.YELLOW + "Thế giới: " + ChatColor.AQUA + world.getName() + "\n" +
                     ChatColor.YELLOW + "Tọa độ: " + ChatColor.GREEN + duelLoc.getBlockX() + ", " + duelLoc.getBlockY() + ", " + duelLoc.getBlockZ() + "\n" +
                     ChatColor.GOLD + "====================================";

        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }

    private Location findSafeLocation(World world, Location center, int radius) {
        for (int attempts = 0; attempts < 40; attempts++) {
            int offsetX = random.nextInt(radius * 2 + 1) - radius;
            int offsetZ = random.nextInt(radius * 2 + 1) - radius;

            int targetX = center.getBlockX() + offsetX;
            int targetZ = center.getBlockZ() + offsetZ;

            if (world.getEnvironment() == World.Environment.NETHER) {
                for (int y = 30; y < 100; y++) {
                    Block block = world.getBlockAt(targetX, y, targetZ);
                    Block above = world.getBlockAt(targetX, y + 1, targetZ);
                    Block above2 = world.getBlockAt(targetX, y + 2, targetZ);

                    if (block.getType().isSolid() && block.getType() != Material.LAVA &&
                        above.getType().isAir() && above2.getType().isAir()) {
                        return new Location(world, targetX + 0.5, y + 1, targetZ + 0.5);
                    }
                }
            } else {
                int highestY = world.getHighestBlockYAt(targetX, targetZ);
                Block blockBelow = world.getBlockAt(targetX, highestY - 1, targetZ);

                if (blockBelow.getType().isSolid() && 
                    blockBelow.getType() != Material.LAVA && 
                    blockBelow.getType() != Material.WATER) {
                    return new Location(world, targetX + 0.5, highestY, targetZ + 0.5);
                }
            }
        }
        return null;
    }
}
