package me.hsgamer.badappleboard;

import me.hsgamer.hscore.minestom.board.Board;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BadAppleBoard {
    public static void main(String[] args) {
        System.setProperty("minestom.tps", "35");
        InputStream inputStream = BadAppleBoard.class.getClassLoader().getResourceAsStream("badapple_en.txt");
        List<Frame> frames = load(inputStream);
        AtomicInteger index = new AtomicInteger(0);
        AtomicBoolean running = new AtomicBoolean(false);

        MinecraftServer minecraftServer = MinecraftServer.init();

        Board board = new Board(
                player -> Component.text("Bad Apple").color(NamedTextColor.RED),
                player -> {
                    Frame frame = frames.get(index.get());
                    return frame.getList();
                }
        );
        var node = MinecraftServer.getGlobalEventHandler();
        Board.hook(node);
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                board.update(player);
            }
        }, TaskSchedule.immediate(), TaskSchedule.nextTick());
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            if (!running.get()) {
                return;
            }
            index.getAndIncrement();
            if (index.get() >= frames.size()) {
                index.set(0);
            }
        }, TaskSchedule.immediate(), TaskSchedule.nextTick());

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));
        node.addListener(PlayerLoginEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 42, 0));
        });
        node.addListener(PlayerTickEvent.class, event -> {
           if (!running.get()) return;
           event.getPlayer().sendActionBar(frames.get(index.get()).getLyric());
        });

        Consumer<CommandSender> addPlayer = sender -> {
            if (sender instanceof Player player) {
                board.addPlayer(player);
            }
        };

        Command command = new Command("start");
        command.setDefaultExecutor((sender, context) -> {
            running.set(!running.get());
            addPlayer.accept(sender);
        });
        MinecraftServer.getCommandManager().register(command);

        Command stopCommand = new Command("stop");
        stopCommand.setDefaultExecutor((sender, context) -> MinecraftServer.stopCleanly());
        MinecraftServer.getCommandManager().register(stopCommand);

        minecraftServer.start("0.0.0.0", 25565);
        Runtime.getRuntime().addShutdownHook(new Thread(MinecraftServer::stopCleanly));
    }

    private static List<Frame> load(InputStream inputStream) {
        List<Frame> list = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            int i = 0;
            String line = bufferedReader.readLine();
            Frame frame = null;
            while (line != null) {
                if (i == 0) {
                    frame = new Frame();
                }
                if (i != 12 && i != 11 && i != 10) {
                    frame.add(Component.text(line).color(NamedTextColor.WHITE));
                }
                if (i == 10) {
                    frame.setLyric(Component.text(line).color(NamedTextColor.WHITE));
                }
                if (i == 12) {
                    list.add(frame);
                }
                i = (i + 1) % 13;
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            list.forEach(Frame::clear);
            list.clear();
        }
        return list;
    }
}
