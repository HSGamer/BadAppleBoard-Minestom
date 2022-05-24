package me.hsgamer.badappleboard;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class Frame {
    private final List<Component> list = new ArrayList<>();
    private Component lyric;

    public List<Component> getList() {
        return list;
    }

    public void add(Component line) {
        list.add(line);
    }

    public void clear() {
        list.clear();
    }

    public Component getLyric() {
        return lyric;
    }

    public void setLyric(Component lyric) {
        this.lyric = lyric;
    }
}
