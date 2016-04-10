package ru.spbau.mit.torrent.client.exec;

import ru.spbau.mit.torrent.client.Client;
import ru.spbau.mit.torrent.client.response.StatResponse;
import ru.spbau.mit.torrent.tracker.FilesProcessor;
import ru.spbau.mit.torrent.tracker.response.ListResponse;
import ru.spbau.mit.torrent.tracker.response.SourcesResponse;
import ru.spbau.mit.torrent.tracker.response.UploadResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class ConsoleClient {
    private static final String HELP_STRING =
            "list (tracker host) (tracker port)\n"
                    + "upload <file path> (tracker host) (tracker port)\n"
                    + "seeds <file id> (tracker host) (tracker port)\n"
                    + "parts <file id> <seed host> <seed port>\n"
                    + "download <file id> <file download path> <file manager prop path> (tracker host) (tracker "
                    + "port)\n"
                    + "distribute <file id> <file path> <file manager prop path>\n"
                    + "remove-distribute <file id> <file manager prop path>\n"
                    + "run <local port> <file manager prop path> (tracker host) (tracker port)";
    private ConsoleClient() {
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
        for (Integer partid : response.getParts()) {
            System.out.println(partid);
        }
    }

    private static boolean tryList(String[] args) {
        if (args[0].equals("list")) {
            Properties props = new Properties();
            if (args.length > 1) {
                props.setProperty("tracker_host", args[1]);
            }
            if (args.length > 2) {
                props.setProperty("tracker_port", args[2]);
            }
            try {
                ListResponse response = new Client(System.err, System.in, props).listRequestToTracker();
                printListResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean tryUpload(String[] args) {
        if (args[0].equals("upload") && args.length >= 2) {
            Path filePath = Paths.get(args[1]);
            Properties props = new Properties();
            if (args.length > 2) {
                props.setProperty("tracker_host", args[2]);
            }
            if (args.length > 3) {
                props.setProperty("tracker_port", args[3]);
            }
            try {
                UploadResponse response = new Client(System.err, System.in, props).uploadNewFileToTracker(filePath
                );
                printUploadResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean trySeeds(String[] args) {
        if (args[0].equals("seeds") && args.length >= 2) {
            Integer fileId = Integer.parseInt(args[1]);
            Properties props = new Properties();
            if (args.length > 2) {
                props.setProperty("tracker_host", args[2]);
            }
            if (args.length > 3) {
                props.setProperty("tracker_port", args[3]);
            }

            SourcesResponse response = new Client(System.err, System.in, props).getFileSeeds(fileId);
            printSourcesResponse(response);
            return true;
        }
        return false;
    }

    private static boolean tryDistribute(String[] args) {
        if (args[0].equals("distribute") && args.length >= 4) {
            Integer fileId = Integer.parseInt(args[1]);
            Path filePath = Paths.get(args[2]);
            Properties props = new Properties();
            props.setProperty("file_manager_path", args[3]);
            try {
                new Client(System.err, System.in, props).addNewDistributionFile(fileId, filePath);
                System.out.println("distribution record added");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean tryRemoveDistribute(String[] args) {
        if (args[0].equals("distribute-remove") && args.length >= 3) {
            Integer fileId = Integer.parseInt(args[1]);
            Properties props = new Properties();
            props.setProperty("file_manager_path", args[2]);
            try {
                new Client(System.err, System.in, props).removeDistribution(fileId);
                System.out.println("distribution record removed");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public static boolean tryRun(String[] args) {
        if (args[0].equals("run") && args.length >= 3) {
            Properties props = new Properties();
            props.setProperty("port", args[1]);
            props.setProperty("file_manager_path", args[2]);
            if (args.length > 3) {
                props.setProperty("tracker_host", args[3]);
            }
            if (args.length > 4) {
                props.setProperty("tracker_port", args[4]);
            }
            new Client(System.err, System.in, props).run();
            return true;
        }
        return false;
    }

    public static boolean tryParts(String[] args) {
        if (args[0].equals("parts") && args.length >= 4) {
            Integer fileId = Integer.parseInt(args[1]);
            try {
                InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(args[2]),
                        Integer.parseInt(args[3]));
                printStatResponse(new Client(System.err, System.in, new Properties()).getFilePartsInfo(fileId,
                        address));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static boolean tryDownload(String[] args) {
        if (args[0].equals("download") && args.length >= 4) {
            int fileId = Integer.parseInt(args[1]);
            Path downloadPath = Paths.get(args[2]);
            Properties props = new Properties();
            props.setProperty("file_manager_path", args[3]);
            if (args.length > 4) {
                props.setProperty("tracker_host", args[4]);
            }
            if (args.length > 5) {
                props.setProperty("tracker_port", args[5]);
            }
            Client client = new Client(System.err, System.in, props);
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

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

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
}
