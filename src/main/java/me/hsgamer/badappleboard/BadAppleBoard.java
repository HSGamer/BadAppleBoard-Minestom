package me.hsgamer.badappleboard;

import fr.noop.subtitle.model.SubtitleCue;
import fr.noop.subtitle.model.SubtitleLine;
import fr.noop.subtitle.model.SubtitleParsingException;
import fr.noop.subtitle.model.SubtitleText;
import fr.noop.subtitle.srt.SrtObject;
import fr.noop.subtitle.srt.SrtParser;
import me.hsgamer.hscore.minestom.board.Board;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerLoginEvent;
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

public class BadAppleBoard {
    public static void main(String[] args) {
        List<Frame> frames;
        if (Boolean.getBoolean("badapple.plain")) {
            System.setProperty("minestom.tps", "35");
            frames = loadPlain();
        } else {
            System.setProperty("minestom.tps", "30");
            frames = loadSRT();
        }
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
                player.sendActionBar(frames.get(index.get()).getLyric());
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

        Command command = new Command("start");
        command.setDefaultExecutor((sender, context) -> running.set(!running.get()));
        MinecraftServer.getCommandManager().register(command);

        Command stopCommand = new Command("stop");
        stopCommand.setDefaultExecutor((sender, context) -> System.exit(0));
        MinecraftServer.getCommandManager().register(stopCommand);

        minecraftServer.start("0.0.0.0", 25565);
        Runtime.getRuntime().addShutdownHook(new Thread(MinecraftServer::stopCleanly));
    }

    private static List<Frame> loadPlain() {
        InputStream inputStream = BadAppleBoard.class.getClassLoader().getResourceAsStream("badapple_en.txt");
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

    private static List<Frame> loadSRT() {
        InputStream inputStream = BadAppleBoard.class.getClassLoader().getResourceAsStream("badapple.srt");
        List<Frame> list = new ArrayList<>();
        SrtParser parser = new SrtParser(StandardCharsets.UTF_8.name());
        try {
            SrtObject subtitle = parser.parse(inputStream);
            for (SubtitleCue cue : subtitle.getCues()) {
                List<SubtitleLine> lines = cue.getLines();
                Frame frame = new Frame();
                for (SubtitleLine line : lines) {
                    StringBuilder builder = new StringBuilder();
                    for (SubtitleText s : line.getTexts()) {
                        builder.append(s.toString());
                    }
                    String text = builder.toString();
                    frame.add(Component.text(text).color(NamedTextColor.WHITE));
                }
                frame.setLyric(Component.empty());
                list.add(frame);
            }
        } catch (IOException | SubtitleParsingException e) {
            e.printStackTrace();
            list.forEach(Frame::clear);
            list.clear();
        }
        return list;
    }
}
