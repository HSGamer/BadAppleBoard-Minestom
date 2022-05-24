package me.hsgamer.badappleboard;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class Frame {
    private final List<Component> list = new ArrayList<>();
    private Component title;

    public List<Component> getList() {
        return list;
    }

    public void add(Component line) {
        list.add(line);
    }

    public void clear() {
        list.clear();
    }

    public Component getTitle() {
        return title;
    }

    public void setTitle(Component title) {
        this.title = title;
    }
}
