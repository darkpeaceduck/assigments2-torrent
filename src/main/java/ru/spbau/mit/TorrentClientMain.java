package ru.spbau.mit;

import ru.spbau.mit.client.Client;
import ru.spbau.mit.client.response.StatResponse;
import ru.spbau.mit.tracker.FilesProcessor;
import ru.spbau.mit.tracker.response.ListResponse;
import ru.spbau.mit.tracker.response.SourcesResponse;
import ru.spbau.mit.tracker.response.UploadResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class TorrentClientMain {
    private static final String HELP_STRING =
            "list (tracker host) (tracker port)\n"
                    + "upload <file path> (tracker host) (tracker port)\n"
                    + "seeds <file id> (tracker host) (tracker port)\n"
                    + "parts <file id> <seed host> <seed port>\n"
                    + "download <file id> <file download path> <file manager prop path> (tracker host) (tracker "
                    + "port)\n"
                    + "distribute <file id> <file path> <file manager prop path>\n"
                    + "remove-distribute <file id> <file manager prop path>\n"
                    + "run <file manager prop path> (local port) (tracker host) (tracker port)";

    private TorrentClientMain() {
    }

    public static void main(String[] args) {
        boolean result = false;
        result |= tryList(args);
        result |= tryUpload(args);
        result |= trySeeds(args);
        result |= tryDistribute(args);
        result |= tryRun(args);
        result |= tryParts(args);
        result |= tryDownload(args);
        result |= tryRemoveDistribute(args);
        if (!result) {
            printHelp();
        }
    }

    private static void printHelp() {
        System.out.println(HELP_STRING);
    }

    private static void printListResponse(ListResponse response) {
        System.out.println(String.format("tracker have %d files to download", response.getFileInfos().size()));
        System.out.println("-------------------------------------------");
        System.out.println("<name> <size> <id>");
        for (FilesProcessor.FileInfo fileInfo : response.getFileInfos()) {
            System.out.println(String.format("%s %d %d", fileInfo.getName(), fileInfo.getSize(), fileInfo.getId())
            );
        }
    }

    private static void printUploadResponse(UploadResponse uploadResponse) {
        System.out.println(String.format("Tracker notified about file, got newid %d", uploadResponse.getId()));
    }

    private static void printSourcesResponse(SourcesResponse response) {
        System.out.println(String.format("%d active seeds to file", response.getSocketAddresses().size()));
        System.out.println("-------------------------------------------");
        System.out.println("<host> <port>");
        for (InetSocketAddress socketAddress : response.getSocketAddresses()) {
            System.out.println(String.format("%s %d", socketAddress.getHostName(), socketAddress.getPort()));
        }
    }

    private static void printStatResponse(StatResponse response) {
        System.out.println(String.format("seed has %d parts of file", response.getParts().size()));
        System.out.println("-------------------------------------------");
        System.out.println("<part id>");
        for (int partid : response.getParts()) {
            System.out.println(partid);
        }
    }

    private static boolean tryList(String[] args) {
        if (args.length > 0 && args[0].equals("list")) {
            ConsoleClientSettings settings = new ConsoleClientSettings();
            if (args.length > 1) {
                settings.setTrackerHost(args[1]);
            }
            if (args.length > 2) {
                settings.setTrackerPort(args[2]);
            }
            try {
                ListResponse response = new Client(System.out, System.in, settings).listRequestToTracker();
                printListResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean tryUpload(String[] args) {
        if (args.length >= 2 && args[0].equals("upload")) {
            Path filePath = Paths.get(args[1]);
            ConsoleClientSettings settings = new ConsoleClientSettings();
            if (args.length > 2) {
                settings.setTrackerHost(args[2]);
            }
            if (args.length > 3) {
                settings.setTrackerPort(args[3]);
            }
            try {
                UploadResponse response = new Client(System.out, System.in, settings)
                        .uploadNewFileToTracker(filePath);
                printUploadResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean trySeeds(String[] args) {
        if (args.length >= 2 && args[0].equals("seeds")) {
            int fileId = Integer.parseInt(args[1]);
            ConsoleClientSettings settings = new ConsoleClientSettings();
            if (args.length > 2) {
                settings.setTrackerHost(args[2]);
            }
            if (args.length > 3) {
                settings.setTrackerPort(args[3]);
            }

            SourcesResponse response = new Client(System.out, System.in, settings).getFileSeeds(fileId);
            printSourcesResponse(response);
            return true;
        }
        return false;
    }

    private static boolean tryDistribute(String[] args) {
        if (args.length >= 4 && args[0].equals("distribute")) {
            int fileId = Integer.parseInt(args[1]);
            Path filePath = Paths.get(args[2]);
            ConsoleClientSettings settings = new ConsoleClientSettings();
            settings.setFileManagerPath(args[3]);
            try {
                new Client(System.out, System.in, settings).addNewDistributionFile(fileId, filePath);
                System.out.println("distribution record added");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean tryRemoveDistribute(String[] args) {
        if (args.length >= 3 && args[0].equals("distribute-remove")) {
            int fileId = Integer.parseInt(args[1]);
            ConsoleClientSettings settings = new ConsoleClientSettings();
            settings.setFileManagerPath(args[2]);
            try {
                new Client(System.out, System.in, settings).removeDistribution(fileId);
                System.out.println("distribution record removed");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean tryRun(String[] args) {
        if (args.length >= 2 && args[0].equals("run")) {
            ConsoleClientSettings settings = new ConsoleClientSettings();
            settings.setFileManagerPath(args[1]);
            if (args.length > 2) {
                settings.setPort(args[2]);
            }
            if (args.length > 3) {
                settings.setTrackerHost(args[3]);
            }
            if (args.length > 4) {
                settings.setTrackerPort(args[4]);
            }
            new Client(System.out, System.in, settings).run();
            return true;
        }
        return false;
    }

    private static boolean tryParts(String[] args) {
        if (args.length >= 4 && args[0].equals("parts")) {
            int fileId = Integer.parseInt(args[1]);
            try {
                InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(args[2]),
                        Integer.parseInt(args[3]));
                printStatResponse(new Client(System.out, System.in, new Properties()).getFilePartsInfo(fileId,
                        address));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean tryDownload(String[] args) {
        if (args.length >= 4 && args[0].equals("download")) {
            int fileId = Integer.parseInt(args[1]);
            Path downloadPath = Paths.get(args[2]);
            ConsoleClientSettings settings = new ConsoleClientSettings();
            settings.setFileManagerPath(args[3]);
            if (args.length > 4) {
                settings.setTrackerHost(args[4]);
            }
            if (args.length > 5) {
                settings.setTrackerPort(args[5]);
            }
            Client client = new Client(System.out, System.in, settings);
            ListResponse response = null;
            try {
                response = client.listRequestToTracker();
                long size = 0;
                for (FilesProcessor.FileInfo fileInfo : response.getFileInfos()) {
                    if (fileInfo.getId() == fileId) {
                        size = fileInfo.getSize();
                    }
                }
                client.addNewReadyToDownloadFile(fileId, size, downloadPath);
                System.out.println(String.format("download record added with filesize = %d", size));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static class ConsoleClientSettings extends Properties {
        private static final String DOWNLOAD_PREFIX = "downloads";
        ConsoleClientSettings() {
            super();
            setProperty("download_prefix", DOWNLOAD_PREFIX);
        }
        public void setFileManagerPath(String arg) {
            setProperty("file_manager_path", arg);
        }

        public void setTrackerHost(String arg) {
            setProperty("tracker_host", arg);
        }

        public void setTrackerPort(String arg) {
            setProperty("tracker_port", arg);
        }

        public void setPort(String arg) {
            setProperty("port", arg);
        }
    }
}
