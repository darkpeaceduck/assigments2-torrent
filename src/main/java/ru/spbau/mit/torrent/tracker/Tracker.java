package ru.spbau.mit.torrent.tracker;

import ru.spbau.mit.torrent.etc.ConfigProcessor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tracker implements Runnable {
    private static final int DEFAULT_PORT = 8081;
    private static final int DEFAULT_THREADS_NUM = 30;
    private static final Path DEFAULT_FILE_INFOS_PATH = Paths.get("src/test/resources/file_infos.properties");
    private static final ExecutorService DEFAULT_CONNECTION_EXECUTOR = Executors
            .newFixedThreadPool(DEFAULT_THREADS_NUM);

    private int port;
    private SharedComponents sharedComponents = new SharedComponents();
    private ExecutorService connectionExecutor;

    private ServerSocket serverSocket;
    private BufferedReader controlInput;

    private Thread childControlThread;

    public Tracker(Path confPath, PrintStream log, InputStream controlInput) {
        sharedComponents.setLog(log);
        this.controlInput = new BufferedReader(new InputStreamReader(controlInput));
        setupDefaults();
        if (confPath != null) {
            try {
                loadConfig(confPath);
            } catch (IOException e) {
                e.printStackTrace(sharedComponents.log);
                setupDefaults();
            }
        }
    }

    private void loadConfig(Path path) throws IOException {
        Properties properties = ConfigProcessor.parseConfig(path);
        if (properties.containsKey("port")) {
            port = Integer.parseInt(properties.getProperty("port"));
        }
        if (properties.containsKey("file_infos_path")) {
            sharedComponents.setFilesProcessor(new FilesProcessor(Paths.get(properties
                    .getProperty("file_infos_path"))));
        }
        if (properties.containsKey("threads")) {
            connectionExecutor = Executors.newFixedThreadPool(Integer.parseInt(properties.getProperty("threads")));
        }
    }

    private void setupDefaults() {
        port = DEFAULT_PORT;
        connectionExecutor = DEFAULT_CONNECTION_EXECUTOR;
        try {
            sharedComponents.setFilesProcessor(new FilesProcessor(DEFAULT_FILE_INFOS_PATH));
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.log);
        }
        sharedComponents.setSeedsProcessor(new SeedsProcessor());
    }

    private void closeAll() {
        try {
            sharedComponents.getFilesProcessor().saveToDrive();
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.getLog());
        }

        try {
            serverSocket.close();
            sharedComponents.log.println("success closed tracker socket");
            childControlThread.join();
        } catch (IOException e) {
            sharedComponents.log.println("failed close socket, now kill child using stop");
            childControlThread.interrupt();
        } catch (InterruptedException e) {
            sharedComponents.log.println("failed join child, now kill child using stop");
            childControlThread.interrupt();
            connectionExecutor.shutdownNow();
        }

        sharedComponents.log.println("OK");
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace(sharedComponents.log);
            return;
        }
        sharedComponents.log.println("success open tracker socket");

        childControlThread = new Thread(new ChildControlRunnable());
        childControlThread.start();

        while (true) {
            String command;
            try {
                command = controlInput.readLine();
                if (command == null) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace(sharedComponents.log);
                continue;
            }

            if (command.equals("quit")) {
                break;
            }
        }

        closeAll();
    }

    public class SharedComponents {
        private PrintStream log;
        private FilesProcessor filesProcessor;
        private SeedsProcessor seedsProcessor;

        public PrintStream getLog() {
            return log;
        }

        public FilesProcessor getFilesProcessor() {
            return filesProcessor;
        }

        public SeedsProcessor getSeedsProcessor() {
            return seedsProcessor;
        }

        public void setLog(PrintStream log) {
            this.log = log;
        }

        public void setFilesProcessor(FilesProcessor filesProcessor) {
            this.filesProcessor = filesProcessor;
        }

        public void setSeedsProcessor(SeedsProcessor seedsProcessor) {
            this.seedsProcessor = seedsProcessor;
        }
    }

    private class ChildControlRunnable implements Runnable {
        @Override
        public void run() {
            sharedComponents.log.println("Child control thread is running now");
            while (!serverSocket.isClosed()) {
                Socket s;
                try {
                    s = serverSocket.accept();
                    connectionExecutor.execute(new ChildHandler(s, sharedComponents));
                } catch (IOException e) {
                    e.printStackTrace(sharedComponents.log);
                    break;
                }
            }
            connectionExecutor.shutdownNow();
            sharedComponents.log.println("Child control thread have quited");
        }
    }
}
