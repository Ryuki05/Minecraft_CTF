package com.example.minecraft_ctf;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.FireworkExplosion;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;

@Mod(Minecraft_ctf.MODID)
public class Minecraft_ctf {
    public static final String MODID = "minecraft_ctf";

    public static final Map<UUID, String> playerTeams = new HashMap<>();
    public static String currentCourse = "NONE";

    private boolean isRedReady = false;
    private boolean isBlueReady = false;
    private boolean isCountingDown = false;

    private static final ResourceLocation ABSOLUTE_SINGLE_BAR_ID =
            ResourceLocation.fromNamespaceAndPath(MODID, "ctf_absolute_single_bar");
    public static int blueGaugeValue = 50;

    public static int selectedTime = 10;
    private static Timer activeGameTimer = null;

    public Minecraft_ctf(IEventBus modEventBus, Dist dist) {
        if (dist == Dist.DEDICATED_SERVER || dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(this::onBlockRightClick);
            NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        var bossBars = server.getCustomBossEvents();

        String[] garbageIds = {
                "ctf_gauge", "red_gauge", "blue_gauge",
                "ctf_single_gauge", "ctf_only_one_bar",
                "layer_red", "layer_blue", "ctf_final_single_bar",
                "ctf_absolute_single_bar"
        };

        for (String id : garbageIds) {
            CustomBossEvent oldBar = bossBars.get(ResourceLocation.fromNamespaceAndPath(MODID, id));
            if (oldBar != null) {
                oldBar.removeAllPlayers();
                bossBars.remove(oldBar);
            }
        }
    }

    private void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player) {

            BlockPos buttonPos = event.getPos();
            BlockState clickedBlock = event.getLevel().getBlockState(buttonPos);

            if (!(clickedBlock.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock)) {
                return;
            }

            MinecraftServer server = player.getServer();
            if (server == null) return;

            // 6方向スキャン
            BlockState targetBlock = Blocks.AIR.defaultBlockState();
            for (Direction dir : Direction.values()) {
                BlockState checkBlock = event.getLevel().getBlockState(buttonPos.relative(dir));
                if (!checkBlock.isAir()) {
                    targetBlock = checkBlock;
                    if (checkBlock.is(Blocks.GREEN_WOOL) || checkBlock.is(Blocks.ORANGE_WOOL) ||
                            checkBlock.is(Blocks.BLACK_WOOL) || checkBlock.is(Blocks.PURPLE_WOOL) ||
                            checkBlock.is(Blocks.RED_WOOL) || checkBlock.is(Blocks.BLUE_WOOL) || checkBlock.is(Blocks.GOLD_BLOCK)) {
                        break;
                    }
                }
            }

            // 🟢 初級コース
            if (clickedBlock.is(Blocks.STONE_BUTTON) && targetBlock.is(Blocks.GREEN_WOOL)) {
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    currentCourse = "CREEPER_SQL";
                    selectedTime = 10;

                    String myTag = player.getTags().contains("RED") ? "RED" : (player.getTags().contains("BLUE") ? "BLUE" : "NONE");
                    player.sendSystemMessage(Component.literal("§a【テスト選択】初級コース（制限時間: 10秒）を選択しました。"));
                    player.teleportTo(overworld, 5778.5, 63.0, -6573.5, java.util.Set.of(), 0.0f, 0.0f);

                    if (!myTag.equals("NONE")) {
                        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                            if (other != player && other.getTags().contains(myTag)) {
                                other.sendSystemMessage(Component.literal("§a【コース選択】相方が初級を選択したため、一緒に移動します！"));
                                other.teleportTo(overworld, 5778.5, 63.0, -6573.5, java.util.Set.of(), 0.0f, 0.0f);
                            }
                        }
                    }
                }
                event.setCanceled(true);
                return;
            }

            // 🟠 中級コース
            if (clickedBlock.is(Blocks.BAMBOO_BUTTON) && targetBlock.is(Blocks.ORANGE_WOOL)) {
                ServerLevel nether = server.getLevel(Level.NETHER);
                if (nether != null) {
                    currentCourse = "MID_LEVEL";
                    selectedTime = 10;

                    String myTag = player.getTags().contains("RED") ? "RED" : (player.getTags().contains("BLUE") ? "BLUE" : "NONE");
                    player.sendSystemMessage(Component.literal("§3【テスト選択】中級コース（制限時間: 10秒）を選択しました。"));
                    player.teleportTo(nether, 109.5, 20.0, 165.5, java.util.Set.of(), 0.0f, 0.0f);

                    if (!myTag.equals("NONE")) {
                        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                            if (other != player && other.getTags().contains(myTag)) {
                                other.sendSystemMessage(Component.literal("§3【コース選択】相方が中級を選択したため、一緒に移動します！"));
                                other.teleportTo(nether, 109.5, 20.0, 165.5, java.util.Set.of(), 0.0f, 0.0f);
                            }
                        }
                    }
                }
                event.setCanceled(true);
                return;
            }

            // ⚫ 上級コース
            if (clickedBlock.is(Blocks.POLISHED_BLACKSTONE_BUTTON) && targetBlock.is(Blocks.BLACK_WOOL)) {
                ServerLevel end = server.getLevel(Level.END);
                if (end != null) {
                    currentCourse = "ADVANCED";
                    selectedTime = 10;

                    String myTag = player.getTags().contains("RED") ? "RED" : (player.getTags().contains("BLUE") ? "BLUE" : "NONE");
                    player.sendSystemMessage(Component.literal("§5【テスト選択】上級コース（制限時間: 10秒）を選択しました。"));
                    player.teleportTo(end, -503.5, 49.0, 9012.5, java.util.Set.of(), 0.0f, 0.0f);

                    if (!myTag.equals("NONE")) {
                        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                            if (other != player && other.getTags().contains(myTag)) {
                                other.sendSystemMessage(Component.literal("§5【コース選択】相方が上級を選択したため、一緒に移動します！"));
                                other.teleportTo(end, -503.5, 49.0, 9012.5, java.util.Set.of(), 0.0f, 0.0f);
                            }
                        }
                    }
                }
                event.setCanceled(true);
                return;
            }

            // 🟣 デバッグコース
            if (clickedBlock.is(Blocks.CHERRY_BUTTON) && targetBlock.is(Blocks.PURPLE_WOOL)) {
                currentCourse = "DEBUG";
                selectedTime = 10;
                player.sendSystemMessage(Component.literal("§d【コース選択】開発テスト用コース（制限時間: 10秒）が選択されました"));
                event.setCanceled(true);
                return;
            }

            // 🔴 レッドチーム選択
            if (clickedBlock.is(Blocks.MANGROVE_BUTTON) && targetBlock.is(Blocks.RED_WOOL)) {
                playerTeams.put(player.getUUID(), "RED");
                player.removeTag("BLUE");
                player.addTag("RED");
                player.sendSystemMessage(Component.literal("§c【選択】レッドチームに参加しました！（REDタグ付与）"));
                event.setCanceled(true);
                return;
            }

            // 🔵 ブルーチーム選択
            if (clickedBlock.is(Blocks.WARPED_BUTTON) && targetBlock.is(Blocks.BLUE_WOOL)) {
                playerTeams.put(player.getUUID(), "BLUE");
                player.removeTag("RED");
                player.addTag("BLUE");
                player.sendSystemMessage(Component.literal("§9【選択】ブルーチームに参加しました！（BLUEタグ付与）"));
                event.setCanceled(true);
                return;
            }

            // 🟡 準備完了ボタン ＆ カウントダウン
            if (clickedBlock.is(Blocks.OAK_BUTTON) && targetBlock.is(Blocks.GOLD_BLOCK)) {
                if (isCountingDown) {
                    player.sendSystemMessage(Component.literal("§cすでにカウントダウンが始まっています！"));
                    event.setCanceled(true);
                    return;
                }

                String myTeam = playerTeams.getOrDefault(player.getUUID(), "NONE");

                if (myTeam.equals("RED")) {
                    if (!isRedReady) {
                        isRedReady = true;
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(Component.literal("§c🔴レッドチームが準備完了しました！"));
                        }
                    }
                } else if (myTeam.equals("BLUE")) {
                    if (!isBlueReady) {
                        isBlueReady = true;
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(Component.literal("§9🔵ブルーチームが準備完了しました！"));
                        }
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§eチームを選択してから準備ボタンを押してください。"));
                    event.setCanceled(true);
                    return;
                }

                if (isRedReady && isBlueReady) {
                    isCountingDown = true;

                    Timer timer = new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        int secondsLeft = 3;

                        @Override
                        public void run() {
                            server.execute(() -> {
                                if (secondsLeft > 0) {
                                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e§l" + secondsLeft)));
                                        p.sendSystemMessage(Component.literal("§eゲーム開始まであと " + secondsLeft + " 秒..."));
                                    }
                                    secondsLeft--;
                                } else {
                                    timer.cancel();

                                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§lGAME START!!")));
                                        p.sendSystemMessage(Component.literal("§6ミニゲームが開始されました！"));
                                    }

                                    var bossBars = server.getCustomBossEvents();
                                    CustomBossEvent existingBar = bossBars.get(ABSOLUTE_SINGLE_BAR_ID);
                                    if (existingBar != null) {
                                        existingBar.removeAllPlayers();
                                        bossBars.remove(existingBar);
                                    }

                                    blueGaugeValue = 50;

                                    CustomBossEvent ctfBar = bossBars.create(ABSOLUTE_SINGLE_BAR_ID, Component.literal("§c◀ RED 50% §f[ 互角 ] §950% BLUE ▶"));
                                    ctfBar.setColor(BossEvent.BossBarColor.BLUE);
                                    ctfBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                                    ctfBar.setMax(100);
                                    ctfBar.setValue(blueGaugeValue);

                                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                        ctfBar.addPlayer(p);
                                    }

                                    startMatchTimer(server);

                                    Timer actionDelayTimer = new Timer();
                                    actionDelayTimer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            server.execute(() -> {
                                                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                                    String team = playerTeams.getOrDefault(p.getUUID(), "NONE");
                                                    if (team.equals("RED")) {
                                                        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c攻めまくろう！")));
                                                    } else if (team.equals("BLUE")) {
                                                        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§9環境を死守せよ！")));
                                                    }
                                                }
                                                isRedReady = false;
                                                isBlueReady = false;
                                                isCountingDown = false;
                                            });
                                        }
                                    }, 1000);
                                }
                            });
                        }
                    }, 0, 1000);
                }
                event.setCanceled(true);
            }
        }
    }

    private static void startMatchTimer(MinecraftServer server) {
        if (activeGameTimer != null) {
            activeGameTimer.cancel();
        }

        activeGameTimer = new Timer();
        activeGameTimer.scheduleAtFixedRate(new TimerTask() {
            int timeLeft = selectedTime;

            @Override
            public void run() {
                server.execute(() -> {
                    if (timeLeft > 0) {
                        int minutes = timeLeft / 60;
                        int seconds = timeLeft % 60;
                        String timeString = String.format("§e⏱ 残り時間: §l%d分%02d秒", minutes, seconds);

                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(Component.literal(timeString), true);
                        }
                        timeLeft--;
                    } else {
                        activeGameTimer.cancel();
                        activeGameTimer = null;

                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§lTIME UP!!")));
                            p.sendSystemMessage(Component.literal("§c制限時間終了！ゲームが幕を閉じました。"));
                            spawnMatchEndFirework(p);
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    private static void spawnMatchEndFirework(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        double centerX = player.getX();
        double targetY = player.getY();
        double centerZ = player.getZ();

        int[] colorPalette = {
                0xFF0000, 0x0000FF, 0x00FF00, 0xFFFF00,
                0xFF00FF, 0x00FFFF, 0xFF6A00, 0xFFFFFF
        };
        int[] fadePalette = { 0xFFAA00, 0xFFD800, 0xFFFFFF };

        int totalShots = 36;
        double radius = 4.0;

        double[] spawnXArray = new double[totalShots];
        double[] spawnZArray = new double[totalShots];
        double[] motionXArray = new double[totalShots];
        double[] motionZArray = new double[totalShots];

        for (int i = 0; i < totalShots; i++) {
            double angle = Math.toRadians(i * (360.0 / totalShots));
            spawnXArray[i] = centerX + (Math.cos(angle) * radius);
            spawnZArray[i] = centerZ + (Math.sin(angle) * radius);
            motionXArray[i] = Math.cos(angle) * 0.05;
            motionZArray[i] = Math.sin(angle) * 0.05;
        }

        Timer scheduler = new Timer();

        for (int i = 0; i < totalShots; i++) {
            final int index = i;
            long delay = index * 25;

            scheduler.schedule(new TimerTask() {
                @Override
                public void run() {
                    level.getServer().execute(() -> {
                        double sx = spawnXArray[index];
                        double sz = spawnZArray[index];
                        double sy = targetY + 0.5;

                        int randomColor = colorPalette[(int)(Math.random() * colorPalette.length)];
                        int randomFade = fadePalette[(int)(Math.random() * fadePalette.length)];

                        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
                        IntList colorsList = new IntArrayList();
                        colorsList.add(randomColor);
                        colorsList.add(0xFFFFFF);

                        IntList fadeColorsList = new IntArrayList();
                        fadeColorsList.add(randomFade);

                        FireworkExplosion explosion = new FireworkExplosion(
                                FireworkExplosion.Shape.LARGE_BALL,
                                colorsList,
                                fadeColorsList,
                                true,
                                true
                        );

                        Fireworks fireworksComponent = new Fireworks(2, List.of(explosion));
                        fireworkStack.set(DataComponents.FIREWORKS, fireworksComponent);

                        FireworkRocketEntity firework = new FireworkRocketEntity(
                                level, sx, sy, sz, fireworkStack
                        );

                        firework.setDeltaMovement(motionXArray[index], 0.45, motionZArray[index]);
                        level.addFreshEntity(firework);
                    });
                }
            }, delay);
        }
    }
}