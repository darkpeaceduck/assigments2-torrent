package ru.spbau.mit;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import ru.spbau.mit.client.Client;
import ru.spbau.mit.tracker.FilesProcessor;
import ru.spbau.mit.tracker.Tracker;
import ru.spbau.mit.tracker.response.ListResponse;
import ru.spbau.mit.tracker.response.SourcesResponse;
import ru.spbau.mit.tracker.response.UploadResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestTorrent {
    private static final Path SRC_PREFIX = Paths.get("src/test");
    private static final int CLIENT1_PORT = 6665;
    private static final int CLIENT2_PORT = 6666;
    private static final int CLIENT3_PORT = 6667;

    private static final Path INITIAL_FILE_PATH = SRC_PREFIX.resolve(Paths.get("resources/initial.jpg"));
    private static final int TRACKER_READY_TIMEOUT = 1000;
    private static final int SLEEPING_TIMEOUT = 50;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path client2FilePath;
    private Path client3FilePath;

    private Client newClient1() throws IOException {
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(CLIENT1_PORT));
        props.setProperty("file_manager_path", String.valueOf(folder.newFile().getAbsolutePath()));
        return new Client(System.err, System.in, props);
    }

    private Client newClient2() throws IOException {
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(CLIENT2_PORT));
        props.setProperty("file_manager_path", String.valueOf(folder.newFile().getAbsolutePath()));

        client2FilePath = folder.newFile().toPath();
        return new Client(System.err, System.in, props);
    }

    private Client newClient3() throws IOException {
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(CLIENT3_PORT));
        props.setProperty("file_manager_path", String.valueOf(folder.newFile().getAbsolutePath()));

        client3FilePath = folder.newFile().toPath();
        return new Client(System.err, System.in, props);
    }

    private Tracker newTracker() throws IOException {
        File trackerProps = folder.newFile();
        Path fileInfosPath = folder.newFile().toPath();
        FileOutputStream stream = new FileOutputStream(trackerProps);
        Properties props = new Properties();
        props.setProperty("file_infos_path", fileInfosPath.toString());
        props.store(stream, "");
        stream.close();

        return new Tracker(trackerProps.toPath(), System.err, System.in);
    }

    @org.junit.Test
    public void test() throws IOException {
        new Thread(newTracker()).start();
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
        client2.addNewReadyToDownloadFile(downloadFileInfo.getId(), downloadFileInfo.getSize(), client2FilePath);
        new Thread(client2).start();

        while (!client2.haveAllFilesDownloaded()) {
            try {
                Thread.sleep(SLEEPING_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        client2.sendSeedInfoToTracker();

        assertTrue(FileUtils.contentEquals(INITIAL_FILE_PATH.toFile(), client2FilePath.toFile()));
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

        client3.addNewReadyToDownloadFile(downloadFileInfo.getId(), downloadFileInfo.getSize(), client3FilePath);
        new Thread(client3).start();

        while (!client3.haveAllFilesDownloaded()) {
            try {
                Thread.sleep(SLEEPING_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertTrue(FileUtils.contentEquals(INITIAL_FILE_PATH.toFile(), client3FilePath.toFile()));
    }
}
