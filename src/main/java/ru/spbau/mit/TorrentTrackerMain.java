package ru.spbau.mit;

import ru.spbau.mit.tracker.Tracker;

import java.nio.file.Paths;

public final class TorrentTrackerMain {
    private TorrentTrackerMain() {
    }
    public static void main(String[] args) {
        new Tracker(Paths.get(args[0]), System.err, System.in).run();
    }
}
