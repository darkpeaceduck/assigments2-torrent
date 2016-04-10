package ru.spbau.mit.torrent.client;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PartsMerger implements Runnable {
    private List<Path> partsPath;
    private Path fileDownloadPath;
    private Client.SharedComponents components;

    public PartsMerger(Client.SharedComponents components, Path fileDownloadPath, List<Path> partsPath) {
        this.components = components;
        this.fileDownloadPath = fileDownloadPath;
        this.partsPath = partsPath;
    }

    @Override
    public void run() {
        components.getLog().println(String.format("Ready to merge parts to %s", fileDownloadPath.toString()));
        try {
            OutputStream outputStream = Files.newOutputStream(fileDownloadPath);
            for (Path path : partsPath) {
                components.getLog().println(String.format("Merging %s ", path.toString()));
                Files.copy(path, outputStream);
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace(components.getLog());
            return;
        }
        components.getLog().println(String.format("Finished merging parts into %s", fileDownloadPath.toString()));
    }
}
