package ru.spbau.mit.torrent;

import org.apache.commons.io.FileUtils;
import ru.spbau.mit.torrent.client.Client;
import ru.spbau.mit.torrent.client.response.StatResponse;
import ru.spbau.mit.torrent.tracker.FilesProcessor;
import ru.spbau.mit.torrent.tracker.Tracker;
import ru.spbau.mit.torrent.tracker.response.ListResponse;
import ru.spbau.mit.torrent.tracker.response.SourcesResponse;
import ru.spbau.mit.torrent.tracker.response.UploadResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestTorrent {
    private static final Path SRC_PREFIX = Paths.get("src/test");
    private static final Path TRACKER_PROPERTIES = SRC_PREFIX.resolve(Paths.get("resources/tracker.properties"));
    private static final Path CLIENT1_FILE_MANAGER_PATH = SRC_PREFIX.resolve(Paths.get("resources/file_manager1"
            + ".properties"));
    private static final Path CLIENT2_FILE_MANAGER_PATH = SRC_PREFIX.resolve(Paths.get("resources/file_manager2"
            + ".properties"));
    private static final Path CLIENT3_FILE_MANAGER_PATH = SRC_PREFIX.resolve(Paths.get("resources/file_manager3"
            + ".properties"));
    private static final int CLIENT1_PORT = 6665;
    private static final int CLIENT2_PORT = 6666;
    private static final int CLIENT3_PORT = 6667;

    private static final Path INITIAL_FILE_PATH = SRC_PREFIX.resolve(Paths.get("resources/initial.jpg"));
    private static final int INITIAL_FILE_PARTS_NUM = 26;
    private static final Path CLIENT2_FILE_PATH = SRC_PREFIX.resolve(Paths.get("resources/client2.jpg"));
    private static final Path CLIENT3_FILE_PATH = SRC_PREFIX.resolve(Paths.get("resources/client3.jpg"));

    private static final int TRACKER_READY_TIMEOUT = 1000;
    private static final int SLEEPING_TIMEOUT = 50;

    private static Client newClient1() {
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(CLIENT1_PORT));
        props.setProperty("file_manager_path", String.valueOf(CLIENT1_FILE_MANAGER_PATH));
        return new Client(System.err, System.in, props);
    }

    private static Client newClient2() {
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(CLIENT2_PORT));
        props.setProperty("file_manager_path", String.valueOf(CLIENT2_FILE_MANAGER_PATH));
        return new Client(System.err, System.in, props);
    }

    private static Client newClient3() {
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(CLIENT3_PORT));
        props.setProperty("file_manager_path", String.valueOf(CLIENT3_FILE_MANAGER_PATH));
        return new Client(System.err, System.in, props);
    }

    @org.junit.Test
    public void test() throws IOException {
        new Thread(new Tracker(TRACKER_PROPERTIES, System.err, System.in)).start();
        try {
            Thread.sleep(TRACKER_READY_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Client client1 = newClient1();
        Client client2 = newClient2();
        Client client3 = newClient3();

        checkWithClient1(client1);
        checkWithClient2(client2);
        checkWithAllClients(client1, client2, client3);
    }

    private void checkWithClient1(Client client1) throws IOException {
        UploadResponse uploadResponse = client1.uploadNewFileToTracker(INITIAL_FILE_PATH);
        assertEquals(uploadResponse.getId(), 0);
        client1.addNewDistributionFile(uploadResponse.getId(), INITIAL_FILE_PATH);
        client1.sendSeedInfoToTracker();
        new Thread(client1).start();
    }

    private void checkWithClient2(Client client2) throws IOException {
        ListResponse response = client2.listRequestToTracker();
        assertEquals(response.getFileInfos().size(), 1);

        FilesProcessor.FileInfo downloadFileInfo = response.getFileInfos().get(0);
        client2.addNewReadyToDownloadFile(downloadFileInfo.getId(), downloadFileInfo.getSize(), CLIENT2_FILE_PATH);
        new Thread(client2).start();

        while (!client2.haveAllFilesDownloaded()) {
            try {
                Thread.sleep(SLEEPING_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        client2.sendSeedInfoToTracker();

        assertTrue(FileUtils.contentEquals(INITIAL_FILE_PATH.toFile(), CLIENT2_FILE_PATH.toFile()));
    }

    private void checkWithAllClients(Client client1, Client client2, Client client3) throws IOException {
        ListResponse response = client3.listRequestToTracker();
        assertEquals(response.getFileInfos().size(), 1);
        FilesProcessor.FileInfo downloadFileInfo = response.getFileInfos().get(0);
        assertEquals(downloadFileInfo.getId(), 0);

        SourcesResponse sourcesResponse = client3.getFileSeeds(downloadFileInfo.getId());
        assertEquals(sourcesResponse.getSocketAddresses().size(), 2);
        Set<Integer> ports = new HashSet<>(Arrays.asList(CLIENT1_PORT, CLIENT2_PORT));
        sourcesResponse.getSocketAddresses().forEach(socketAddress -> {
            assertEquals(socketAddress.getAddress().getHostAddress(), "127.0.0.1");
            assertTrue(ports.contains(socketAddress.getPort()));
            ports.remove(socketAddress.getPort());
        });

        final int delPartition = 10;
        for (int i = 0; i < delPartition; i++) {
            client1.removeDistributionPart(downloadFileInfo.getId(), i);
        }
        StatResponse statResponse = client3.getFilePartsInfo(downloadFileInfo.getId(),
                new InetSocketAddress(InetAddress.getLocalHost(), CLIENT1_PORT));

        assertEquals(statResponse.getParts(),
                Stream.iterate(delPartition, x -> x + 1).limit(INITIAL_FILE_PARTS_NUM - delPartition)
                        .collect(Collectors.toList()));

        for (int i = delPartition; i < INITIAL_FILE_PARTS_NUM; i++) {
            client2.removeDistributionPart(downloadFileInfo.getId(), i);
        }

        statResponse = client3.getFilePartsInfo(downloadFileInfo.getId(),
                new InetSocketAddress(InetAddress.getLocalHost(), CLIENT2_PORT));

        assertEquals(statResponse.getParts(), Stream.iterate(0, x -> x + 1).limit(delPartition).collect(Collectors
                .toList()));
        client3.addNewReadyToDownloadFile(downloadFileInfo.getId(), downloadFileInfo.getSize(), CLIENT3_FILE_PATH);
        new Thread(client3).start();

        while (!client3.haveAllFilesDownloaded()) {
            try {
                Thread.sleep(SLEEPING_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertTrue(FileUtils.contentEquals(INITIAL_FILE_PATH.toFile(), CLIENT3_FILE_PATH.toFile()));
    }
}
