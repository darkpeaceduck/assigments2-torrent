package ru.spbau.mit.client;

import ru.spbau.mit.client.request.GetRequest;
import ru.spbau.mit.client.response.StatResponse;
import ru.spbau.mit.tracker.request.ListRequest;
import ru.spbau.mit.tracker.request.UpdateRequest;
import ru.spbau.mit.tracker.request.UploadRequest;
import ru.spbau.mit.tracker.response.ListResponse;
import ru.spbau.mit.tracker.response.SourcesResponse;
import ru.spbau.mit.tracker.response.UploadResponse;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements Runnable {
    private static final long DEFAULT_PART_SIZE = 1L << 20;
    private static final long DEFAULT_PART_FILE_SIZE = 1L << 30;
    private static final int DEFAULT_PORT = 0;
    private static final int DEFAULT_THREADS_NUM = 10;
    private static final InetSocketAddress DEFAULT_TRACKER_ADRESS = new InetSocketAddress("127.0.0.1", 8081);
    private static final Path DEFAULT_FILE_MANAGER_PROPS_PATH = Paths.get("src/test/resources/file_manager"
            + ".properties");
    private static final ExecutorService DEFAULT_CONNECTION_EXECUTOR = Executors
            .newFixedThreadPool(DEFAULT_THREADS_NUM);
    private static final Path DEFAULT_DOWNLOAD_PREFIX = Paths.get(".");

    private static final int DOWNLOAD_TRHEAD_SLEEP_TIMEOUT = 1000;


    private SharedComponents sharedComponents = new SharedComponents();
    private int port;
    private InetSocketAddress trackerAdress = null;
    private ExecutorService tasksExecutor;
    private Path downloadPrefix;

    private ServerSocket serverSocket;
    private BufferedReader controlInput;

    private Thread seedControlThread;
    private Timer trackerUpdaterTimer;
    private Thread downloadControlThread;

    private AtomicBoolean haveDownloadedAllFiles = new AtomicBoolean(false);

    public Client(PrintStream log, InputStream controlInput, Properties props) {
        sharedComponents.setLog(log);
        this.controlInput = new BufferedReader(new InputStreamReader(controlInput));
        setupDefaults();
        loadProps(props);
    }

    public ListResponse listRequestToTracker() throws IOException {
        TrackerConnection connection = new TrackerConnection(trackerAdress);
        connection.getRequestWriter().writeRequest(new ListRequest());

        ListResponse response = connection.getResponseReader().nextListResponse();

        try {
            connection.closeAll();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }

        return response;
    }

    public boolean haveAllFilesDownloaded() {
        return haveDownloadedAllFiles.get();
    }

    public UploadResponse uploadNewFileToTracker(Path path) throws IOException {
        TrackerConnection connection = new TrackerConnection(trackerAdress);

        UploadRequest request = new UploadRequest();
        request.setName(path.getFileName().toString());
        request.setSize(Files.size(path));

        connection.getRequestWriter().writeRequest(request);

        UploadResponse response = connection.getResponseReader().nextUploadResponse();

        try {
            connection.closeAll();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }

        return response;
    }

    public SourcesResponse getFileSeeds(int id) {
        return ClientTasks.collectSeedInfos(sharedComponents, trackerAdress, id);
    }

    public void addNewDistributionFile(int fileId, Path path) throws IOException {
        sharedComponents.getFilesManager().insertNewDistributedFile(fileId, path);
        sharedComponents.getFilesManager().saveToDrive();
    }

    public void removeDistribution(int fileId) throws IOException {
        sharedComponents.getFilesManager().removeDistributedFile(fileId);
        sharedComponents.getFilesManager().saveToDrive();
    }

    public void removeDistributionPart(int fileId, int partId) throws IOException {
        sharedComponents.getFilesManager().removeDistributedFileEntry(fileId, partId);
        sharedComponents.getFilesManager().saveToDrive();
    }

    public void addNewReadyToDownloadFile(int fileId, long size, Path downloadPath) throws IOException {
        downloadPath = downloadPrefix.resolve(downloadPath);
        sharedComponents.getFilesManager().insertNewReadyToDownloadFile(fileId, new FilesManager
                .ReadyToDownloadFilesEntry(downloadPath, size));
        sharedComponents.getFilesManager().saveToDrive();
    }

    public StatResponse getFilePartsInfo(int fileId, InetSocketAddress seed) {
        return ClientTasks.collectFilePartInfos(sharedComponents, seed, fileId);
    }

    public void sendSeedInfoToTracker() {
        try {
            sharedComponents.log.println("updating info to tracker");
            TrackerConnection connection = new TrackerConnection(trackerAdress);
            UpdateRequest request = new UpdateRequest();
            request.setIds(sharedComponents.getFilesManager()
                    .getAvailableFileIds());
            request.setSeedPort((short) port);
            connection.getRequestWriter().writeRequest(request);
            connection.closeAll();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.log);
        }
    }

    public List<FilesManager.FileInProcessInfo> getFilesFilesInProcessInfo() {
        return sharedComponents.getFilesManager().getFileInProcessInfo();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.log);
            return;
        }
        port = serverSocket.getLocalPort();
        sharedComponents.log.println("success open tracker socket");
        sendSeedInfoToTracker();

        seedControlThread = new Thread(new SeedControlRunnable());
        trackerUpdaterTimer = new Timer();
        downloadControlThread = new Thread(new DownloadManagerRunnable());

        seedControlThread.start();

        final int trackerUpdateTimerSheduleTimeout = 30 * 1000;
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                sendSeedInfoToTracker();
            }
        };
        trackerUpdaterTimer.schedule(task, trackerUpdateTimerSheduleTimeout);

        downloadControlThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::closeAll));
    }

    private void loadProps(Properties properties) {
        if (properties.containsKey("tracker_port")) {
            trackerAdress = new InetSocketAddress(trackerAdress.getAddress(), Integer.parseInt(properties
                    .getProperty("tracker_port")));
        }
        if (properties.containsKey("tracker_host")) {
            try {
                trackerAdress = new InetSocketAddress(InetAddress.getByName(properties.getProperty(
                        "tracker_host")),
                        trackerAdress.getPort());
            } catch (UnknownHostException e) {
                e.printStackTrace(sharedComponents.getLog());
            }
        }
        if (properties.containsKey("port")) {
            port = Integer.parseInt(properties.getProperty("port"));
        }
        if (properties.containsKey("file_manager_path")) {
            try {
                sharedComponents.setFilesManager(new FilesManager(Paths.get(properties.getProperty(
                        "file_manager_path")), DEFAULT_PART_SIZE, DEFAULT_PART_FILE_SIZE));
            } catch (IOException e) {
                e.printStackTrace(sharedComponents.getLog());
            }
        }
        if (properties.containsKey("threads")) {
            tasksExecutor = Executors.newFixedThreadPool(Integer.parseInt(properties.getProperty("threads")));
        }
        if (properties.containsKey("download_prefix")) {
            downloadPrefix = Paths.get(properties.getProperty("download_prefix"));
        }
    }

    private void setupDefaults() {
        port = DEFAULT_PORT;
        try {
            sharedComponents.setFilesManager(new FilesManager(DEFAULT_FILE_MANAGER_PROPS_PATH,
                    DEFAULT_PART_SIZE, DEFAULT_PART_FILE_SIZE));
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }
        tasksExecutor = DEFAULT_CONNECTION_EXECUTOR;
        trackerAdress = DEFAULT_TRACKER_ADRESS;
        downloadPrefix = DEFAULT_DOWNLOAD_PREFIX;
    }

    private void closeAll() {
        try {
            sharedComponents.getFilesManager().saveToDrive();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }

        try {
            serverSocket.close();
            sharedComponents.log.println("success closed seed socket");
            seedControlThread.join();
        } catch (IOException e) {
            sharedComponents.log.println("failed close socket, now kill child using stop");
            seedControlThread.interrupt();
        } catch (InterruptedException e) {
            sharedComponents.log.println("failed join child, now kill child using stop");
            seedControlThread.interrupt();
            tasksExecutor.shutdownNow();
        }

        downloadControlThread.interrupt();

        trackerUpdaterTimer.cancel();
        trackerUpdaterTimer.purge();
        sharedComponents.log.println("OK");
    }

    public class SharedComponents {

        private PrintStream log;
        private PrintStream statusLog;

        private FilesManager filesManager;

        public FilesManager getFilesManager() {
            return filesManager;
        }

        public void setFilesManager(FilesManager filesManager) {
            this.filesManager = filesManager;
        }

        public PrintStream getLog() {
            return log;
        }

        public void setLog(PrintStream log) {
            this.log = log;
        }
    }

    private class DownloadManagerRunnable implements Runnable {
        private Map<Integer, FilesManager.ReadyToDownloadFilesEntry> readyToDownloadFiles;
        private Map<Integer, Future<SourcesResponse>> pendingSeedsInfoTasks;
        private Map<Integer, Map<InetSocketAddress, Future<StatResponse>>> pendingPartsInfoTasks;
        private Map<Integer, Map<Integer, Future<?>>> pendingDownloadTasks;
        private Map<Integer, Future<?>> pendingMergeTasks;

        @Override
        public void run() {
            while (true) {
                clean();
                while (sharedComponents.getFilesManager().getReadyToDownloadFiles().isEmpty()) {
                    try {
                        Thread.sleep(DOWNLOAD_TRHEAD_SLEEP_TIMEOUT);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                haveDownloadedAllFiles.set(false);
                prepareAndSubmitSeedsInfoTasks();
                getSeedsInfoTasksAndSubmitPartsInfoTasks();
                getPartsInfoTasksAndSubmitDownloadTasks();
                downloadAllAndSubmitMergePartsTasks();
                mergeAllAndSendComplitedToFilesManager();
                sharedComponents.log.println("All Files have downloaded");
                haveDownloadedAllFiles.set(true);
            }
        }

        private void clean() {
            pendingSeedsInfoTasks = new HashMap<>();
            pendingPartsInfoTasks = new HashMap<>();
            pendingDownloadTasks = new HashMap<>();
            pendingMergeTasks = new HashMap<>();
        }

        private void prepareAndSubmitSeedsInfoTasks() {
            readyToDownloadFiles = sharedComponents.getFilesManager()
                    .getReadyToDownloadFiles();

            readyToDownloadFiles.entrySet().forEach(entry -> {
                int fileId = entry.getKey();
                pendingSeedsInfoTasks.put(fileId, tasksExecutor.submit(() ->
                        ClientTasks.collectSeedInfos(sharedComponents, trackerAdress, fileId)));
            });
        }

        private void submitDownloadTasks(int fileId, Map<Integer, InetSocketAddress> partsSeedAdress) {
            pendingDownloadTasks.put(fileId, new TreeMap<>());
            partsSeedAdress.entrySet().forEach(entry -> {
                int partId = entry.getKey();
                GetRequest getRequest = new GetRequest();
                getRequest.setId(fileId);
                getRequest.setPart(entry.getKey());
                pendingDownloadTasks.get(fileId).put(partId, tasksExecutor.submit(() -> ClientTasks.downloadPart(
                        sharedComponents.getFilesManager().getPartFilePath(readyToDownloadFiles.get(fileId),
                                partId),
                        sharedComponents.getFilesManager().getPartOffsetInsidePartsDownloadFile(partId),
                        sharedComponents.getFilesManager().getPartSize(readyToDownloadFiles.get(fileId), partId),
                        getRequest,
                        entry.getValue(),
                        sharedComponents)));
            });
        }

        private void submitPartsInfoTasks(int fileId, SourcesResponse sourcesResponse) {
            if (sourcesResponse != null) {
                pendingPartsInfoTasks.put(fileId, new HashMap<>());
                for (InetSocketAddress socketAddress : sourcesResponse.getSocketAddresses()) {
                    pendingPartsInfoTasks.get(fileId).put(socketAddress, tasksExecutor.submit(() ->
                            ClientTasks.collectFilePartInfos(sharedComponents, socketAddress, fileId)));
                }
            }
        }

        private void getPartsInfoTasksAndSubmitDownloadTasks() {
            pendingPartsInfoTasks.entrySet().forEach(entry -> {
                int fileId = entry.getKey();
                Map<Integer, InetSocketAddress> partsAdresses = new HashMap<>();
                int needPartsSize = sharedComponents.getFilesManager().getFilePartsNum(readyToDownloadFiles.get(
                        fileId));
                for (Map.Entry<InetSocketAddress, Future<StatResponse>> addressesEntry
                        : entry.getValue().entrySet()) {
                    if (partsAdresses.size() == needPartsSize) {
                        break;
                    }
                    StatResponse response = null;
                    InetSocketAddress socketAddress = addressesEntry.getKey();
                    try {
                        response = addressesEntry.getValue().get();
                    } catch (Exception e) {
                        e.printStackTrace(sharedComponents.getLog());
                        continue;
                    }
                    for (int newPartId : response.getParts()) {
                        partsAdresses.put(newPartId, socketAddress);
                    }
                }

                if (partsAdresses.size() == needPartsSize) {
                    submitDownloadTasks(fileId, partsAdresses);
                }
            });
        }

        private void getSeedsInfoTasksAndSubmitPartsInfoTasks() {
            pendingSeedsInfoTasks.entrySet().forEach(entry -> {
                SourcesResponse sourcesResponse = null;
                int fileId = entry.getKey();
                try {
                    sourcesResponse = (SourcesResponse) entry.getValue().get();
                    submitPartsInfoTasks(fileId, sourcesResponse);
                } catch (Exception e) {
                    e.printStackTrace(sharedComponents.getLog());
                }
            });
        }

        private void downloadAllAndSubmitMergePartsTasks() {
            pendingDownloadTasks.entrySet().forEach(entry -> {
                int fileId = entry.getKey();
                Set<Path> partsPaths = new TreeSet<Path>();
                entry.getValue().entrySet().forEach(partsEntry -> {
                    try {
                        partsEntry.getValue().get();
                        int partId = partsEntry.getKey();
                        Path partFilePath = sharedComponents.getFilesManager()
                                .getPartFilePath(readyToDownloadFiles.get(fileId), partId);
                        if (!partsPaths.contains(partFilePath)) {
                            partsPaths.add(partFilePath);
                        }
                        sharedComponents.getFilesManager().insertNewDistributedPart(fileId, partId,
                                partFilePath);
                    } catch (Exception e) {
                        e.printStackTrace(sharedComponents.getLog());
                    }
                });
                pendingMergeTasks.put(entry.getKey(), tasksExecutor.submit(() -> ClientTasks.mergeParts(
                        sharedComponents,
                        readyToDownloadFiles.get(fileId).getDownloadPath(),
                        partsPaths)));
            });
        }

        private void mergeAllAndSendComplitedToFilesManager() {
            pendingMergeTasks.entrySet().forEach(entry -> {
                try {
                    int fileId = entry.getKey();
                    int partsNum = sharedComponents.getFilesManager().getFilePartsNum(
                            readyToDownloadFiles.get(entry.getKey()));
                    entry.getValue().get();
                    for (int partId = 0; partId < partsNum; partId++) {
                        Path partPath = sharedComponents.getFilesManager()
                                .getPartFilePath(readyToDownloadFiles.get(fileId),
                                partId);
                        sharedComponents.getFilesManager().removeDistributedFileEntry(fileId, partId);
                        try {
                            Files.delete(partPath);
                        } catch (Exception e) {
                        }
                    }
                    sharedComponents.getFilesManager().insertNewDistributedFile(fileId,
                            readyToDownloadFiles.get(fileId).getDownloadPath());

                    sharedComponents.getFilesManager().cleanWithDownloadComplete(fileId);
                } catch (Exception e) {
                    e.printStackTrace(sharedComponents.getLog());
                }
            });
        }
    }

    private class SeedControlRunnable implements Runnable {
        @Override
        public void run() {
            sharedComponents.log.println("Child control thread is running now");
            while (!serverSocket.isClosed()) {
                Socket s;
                try {
                    s = serverSocket.accept();
                    tasksExecutor.execute(new SeedHandler(s, sharedComponents));
                } catch (IOException e) {
                    e.printStackTrace(sharedComponents.log);
                    break;
                }
            }
            tasksExecutor.shutdownNow();
            sharedComponents.log.println("Child control thread have quited");
        }
    }
}
