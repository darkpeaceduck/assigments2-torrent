package ru.spbau.mit.ui;


import ru.spbau.mit.client.Client;
import ru.spbau.mit.tracker.response.ListResponse;
import ru.spbau.mit.tracker.response.SourcesResponse;
import ru.spbau.mit.tracker.response.UploadResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Collectors;

public class TorrentCientUIMain {
    private static final String FRAME_TAG = "Client";
    private static final int FRAME_DEFAULT_WIDTH = 1200;
    private static final int FRAME_DEFAULT_HEIGHT = 600;
    private static final Path CLIENT_PROPERTIES_SUFFIX = Paths.get("client.properties");
    private static final Path CLIENT_DOWNLOADS_SUFFIX = Paths.get("downloads/");

    private static final String MAIN_MENU_TAG = "Menu";
    private static final String SET_WORKING_DIR_MENU_ITEM_TAG = "Set working dir...";
    private static final String RUN_MENU_ITEM_TAG = "Start with tracker...";
    private static final String SET_LOCAL_PORT_MENU_ITEM_TAG = "Set local port ...";

    private static final String SET_WORKING_DIR_DLG_MSG = "Enter new working dir";
    private static final String SET_TRACKER_HOST_ADDR_DLG_MSG = "Enter tracker host";
    private static final String SET_TRACKER_PORT_ADDR_DLG_MSG = "Enter tracker port";
    private static final String SET_LOCAL_PORT = "Enter local port";

    private static final String TRACKER_TAB_TAG = "Tracker";
    private static final String FILES_TAB_TAG = "Files";

    private static final String REFRESH_BUTTON_TAG = "Refresh";
    private static final String UPLOAD_BUTTON_TAG = "Upload";

    private static final String TABLE_TAB_TORRENT_TAG = "table";
    private static final String TABLE_TAB_FILES_TAG = "table";

    private static final String UPLOADING_STATUS = "Uploading";
    private static final String DOWNLOADING_STATUS = "Downloading";

    private static final String DOWLOAD_POPUP_TRACKER_TAB = "Download this file...";
    private static final String DISTRIBUTE_POPUP_TRACKER_TAB = "Distribute this file...";
    private static final String REMOVE_DISTRIBUTE_POPUP_FILES_TAB = "Remove distribution of this file...";

    private static final Path DEFAULT_WORKING_DIR_PATH = Paths.get("client");

    private Path workingDir = DEFAULT_WORKING_DIR_PATH;
    private InetSocketAddress trackerAddr;
    private Client client;
    private int localPort = 0;

    private JFrame frame;

    public TorrentCientUIMain(JFrame frame) {
        createTorrentClientSettings();

        this.frame = frame;
        final JTabbedPane pane = new JTabbedPane();

        createTabs(pane);

        disableTabs(pane);

        frame.add(pane);

        frame.setJMenuBar(buildMenuBar(pane));

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.setSize(FRAME_DEFAULT_WIDTH, FRAME_DEFAULT_HEIGHT);
        frame.setResizable(true);
        frame.setVisible(true);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TorrentCientUIMain(new JFrame(FRAME_TAG)));
    }

    private void createTabs(JTabbedPane pane) {
        final JPanel trackerTab = new JPanel();
        final JPanel filesTab = new JPanel();
        trackerTab.setLayout(new BoxLayout(trackerTab, BoxLayout.Y_AXIS));
        filesTab.setLayout(new BoxLayout(filesTab, BoxLayout.Y_AXIS));

        pane.addTab(TRACKER_TAB_TAG, trackerTab);
        pane.addTab(FILES_TAB_TAG, filesTab);

        createTabTorrentButtons(pane);
    }

    private void disableTabs(JTabbedPane pane) {
        JPanel trackerTab = (JPanel) pane.getComponentAt(pane.indexOfTab(TRACKER_TAB_TAG));
        for (Component comp : trackerTab.getComponents()) {
            comp.setVisible(false);
        }
        pane.setEnabledAt(pane.indexOfTab(FILES_TAB_TAG), false);
        pane.setEnabledAt(pane.indexOfTab(TRACKER_TAB_TAG), false);
    }

    private void createTabTorrentButtons(JTabbedPane pane) {
        JPanel trackerTab = (JPanel) pane.getComponentAt(pane.indexOfTab(TRACKER_TAB_TAG));

        final JButton buttonRefresh = new JButton(REFRESH_BUTTON_TAG);
        final JButton buttonUpload = new JButton(UPLOAD_BUTTON_TAG);
        buttonRefresh.addActionListener(e -> {
            client.sendSeedInfoToTracker();
            refreshAllTabs(pane);
        });
        buttonUpload.addActionListener(e -> uploadFileToTracker(pane));
        trackerTab.add(buttonRefresh);
        trackerTab.add(buttonUpload);
    }
    private void createTorrentClientSettings() {
        makeNewWorkingDir(workingDir);
    }

    private void refreshAllTabs(JTabbedPane pane) {
        refreshTrackerTab(pane);
        refreshFilesTab(pane);
    }

    private JMenu buildJMenu(JTabbedPane pane) {
        JMenuItem setWorkingDirMenuItem = new JMenuItem(SET_WORKING_DIR_MENU_ITEM_TAG);
        setWorkingDirMenuItem.addActionListener(e -> onSetWorkingDir(pane));

        JMenuItem runMenuItem = new JMenuItem(RUN_MENU_ITEM_TAG);
        runMenuItem.addActionListener(e -> onRun(pane));

        JMenuItem setLocalPort = new JMenuItem(SET_LOCAL_PORT_MENU_ITEM_TAG);
        setLocalPort.addActionListener(e -> setLocalPort(pane));

        JMenu jMenu = new JMenu(MAIN_MENU_TAG);
        jMenu.add(setWorkingDirMenuItem);
        jMenu.add(runMenuItem);
        jMenu.add(setLocalPort);
        return jMenu;
    }

    private JMenuBar buildMenuBar(JTabbedPane pane) {
        JMenuBar jMenuBar = new JMenuBar();
        jMenuBar.add(buildJMenu(pane));
        return jMenuBar;
    }

    private void setLocalPort(JTabbedPane pane) {
        String inputPort = JOptionPane.showInputDialog(pane, SET_LOCAL_PORT);
        try {
            localPort = Integer.parseInt(inputPort);
            JOptionPane.showMessageDialog(frame, "Success set new local port " + String.valueOf(localPort));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, e.toString());
        }
    }

    private void makeNewWorkingDir(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
            if (!Files.exists(path.resolve(CLIENT_PROPERTIES_SUFFIX))) {
                Files.createFile(path.resolve(CLIENT_PROPERTIES_SUFFIX));
            }
            if (!Files.exists(path.resolve(CLIENT_DOWNLOADS_SUFFIX))) {
                Files.createDirectory(path.resolve(CLIENT_DOWNLOADS_SUFFIX));
            }
            workingDir = path;
            JOptionPane.showMessageDialog(frame, "Success make new working dir in " + path.toString());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, e.toString());
        }
    }

    private void setTrackerAddr(String host, String port) {
        try {
            trackerAddr = new InetSocketAddress(host, Integer.parseInt(port));
            JOptionPane.showMessageDialog(frame, "Success set tracker addr with " + host.toString() + ":" + port
                    .toString());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, e.toString());
        }
    }

    private void onSetWorkingDir(JTabbedPane pane) {
        String inputStr = JOptionPane.showInputDialog(pane, SET_WORKING_DIR_DLG_MSG);
        if (inputStr != null && inputStr.length() > 0) {
            makeNewWorkingDir(Paths.get(inputStr));
        }
    }

    private void enableTabs(JTabbedPane pane) {
        JPanel trackerTab = (JPanel) pane.getComponentAt(pane.indexOfTab(TRACKER_TAB_TAG));
        for (Component comp : trackerTab.getComponents()) {
            comp.setVisible(true);
        }

        frame.getJMenuBar().setVisible(false);

        pane.setEnabledAt(pane.indexOfTab(FILES_TAB_TAG), true);
        pane.setEnabledAt(pane.indexOfTab(TRACKER_TAB_TAG), true);
    }

    private void initTabFilesRefresher(JTabbedPane pane) {
        final int timeout = 100;
        Timer refreshingFilesTab = new Timer(timeout, e -> {
            refreshFilesTab(pane);
        });
        refreshingFilesTab.start();
    }

    private void onRun(JTabbedPane pane) {
        String inputHost = JOptionPane.showInputDialog(pane, SET_TRACKER_HOST_ADDR_DLG_MSG);
        String inputPort = JOptionPane.showInputDialog(pane, SET_TRACKER_PORT_ADDR_DLG_MSG);
        setTrackerAddr(inputHost, inputPort);

        enableTabs(pane);
        initTabFilesRefresher(pane);
        clientRun(pane);
    }

    private void clientRun(JTabbedPane pane) {
        Properties props = new Properties();
        props.setProperty("file_manager_path", workingDir.resolve(CLIENT_PROPERTIES_SUFFIX).toString());
        props.setProperty("download_prefix", workingDir.resolve(CLIENT_DOWNLOADS_SUFFIX).toString());
        props.setProperty("port", String.valueOf(localPort));
        if (trackerAddr != null) {
            props.setProperty("tracker_host", trackerAddr.getHostName());
            props.setProperty("tracker_port", String.valueOf(trackerAddr.getPort()));
        }
        client = new Client(System.out, System.in, props);

        new Thread(client).start();
        refreshAllTabs(pane);
    }

    private void refreshTrackerTab(JTabbedPane pane) {
        JPanel trackerTab = (JPanel) pane.getComponentAt(pane.indexOfTab(TRACKER_TAB_TAG));

        for (Component comp : trackerTab.getComponents()) {
            if (comp.getName() != null && comp.getName().equals(TABLE_TAB_TORRENT_TAG)) {
                trackerTab.remove(comp);
                break;
            }
        }
        pane.repaint();
        new Thread(() -> {
            try {
                final ListResponse response = client.listRequestToTracker();
                SwingUtilities.invokeLater(() -> {
                    JScrollPane wrappedPane = new JScrollPane(createTableTabTorrent(response.getFileInfos()
                            .stream()
                            .map(e -> {
                                SourcesResponse resp = client.getFileSeeds(e.getId());
                                return new Object[]{e.getId(), e.getName(), e.getSize(),
                                        resp.getSocketAddresses()};
                            })
                            .collect(Collectors.toList()).toArray(new Object[][]{})));
                    wrappedPane.setName(TABLE_TAB_TORRENT_TAG);
                    trackerTab.add(wrappedPane);
                    pane.repaint();
                });
            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, e.toString()));
            }
        }).start();
    }

    private void refreshFilesTab(JTabbedPane pane) {
        JPanel filesTab = (JPanel) pane.getComponentAt(pane.indexOfTab(FILES_TAB_TAG));

        for (Component comp : filesTab.getComponents()) {
            if (comp.getName() != null && comp.getName().equals(TABLE_TAB_FILES_TAG)) {
                filesTab.remove(comp);
                break;
            }
        }
        JScrollPane wrappedPane = new JScrollPane(createTableTabFiles(client.getFilesFilesInProcessInfo().stream()
                .map(e -> {
                    return new Object[]{e.getId(), e.getLabel(), e.getPercentStatus() == 100f ? UPLOADING_STATUS
                            : DOWNLOADING_STATUS, e.getPercentStatus()};
                })
                .collect(Collectors.toList()).toArray(new Object[][]{})));
        wrappedPane.setName(TABLE_TAB_FILES_TAG);
        filesTab.add(wrappedPane);
        pane.repaint();

    }

    private void uploadFileToTracker(JTabbedPane pane) {
        final JFileChooser fc = new JFileChooser();
        int retval = fc.showOpenDialog(pane);
        if (retval == JFileChooser.APPROVE_OPTION) {
            final Path path = fc.getSelectedFile().toPath();
            new Thread(() -> {
                try {
                    UploadResponse resp = client.uploadNewFileToTracker(path);
                    client.addNewDistributionFile(resp.getId(), path);
                    client.sendSeedInfoToTracker();
                    SwingUtilities.invokeLater(() -> {
                        refreshAllTabs(pane);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, e.toString()));
                }
            }).start();
        }
    }

    private int getTableSelectedRowOnMouseEvent(JTable table, MouseEvent e) {
        int r = table.rowAtPoint(e.getPoint());
        if (r >= 0 && r < table.getRowCount()) {
            table.setRowSelectionInterval(r, r);
        } else {
            table.clearSelection();
        }

        return table.getSelectedRow();
    }

    private JTable createTableTabTorrent(Object[][] data) {
        final String[] columnsNames = {
                "id",
                "name",
                "size",
                "peers"
        };
        final JTable table = new JTable(data, columnsNames);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int rowindex = getTableSelectedRowOnMouseEvent(table, e);
                if (rowindex >= 0 && e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    JPopupMenu popup = createTrackerTabPopup(table, rowindex);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        return table;
    }

    private JTable createTableTabFiles(Object[][] data) {
        final String[] columnsNames = {
                "id",
                "label",
                "status",
                "%",
        };
        final JTable table = new JTable(data, columnsNames);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int rowindex = getTableSelectedRowOnMouseEvent(table, e);
                if (rowindex < 0) {
                    return;
                }
                int id = (int) table.getModel().getValueAt(rowindex, 0);
                String status = (String) table.getModel().getValueAt(rowindex, 2);

                if (status.equals(UPLOADING_STATUS) && e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    JPopupMenu popup = createFilesTabPopup(id);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        return table;
    }

    private JPopupMenu createTrackerTabPopup(final JTable table, final int rowindex) {
        int id = (int) table.getModel().getValueAt(rowindex, 0);
        Path path = Paths.get((String) table.getModel().getValueAt(rowindex, 1));
        long size = (long) table.getModel().getValueAt(rowindex, 2);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemDownload = new JMenuItem(DOWLOAD_POPUP_TRACKER_TAB);
        itemDownload.addActionListener(e -> {
            try {
                client.addNewReadyToDownloadFile(id, size, path);
            } catch (IOException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(frame, e1.getMessage());
            }
        });
        JMenuItem itemDistribute = new JMenuItem(DISTRIBUTE_POPUP_TRACKER_TAB);
        itemDistribute.addActionListener(e -> {
            final JFileChooser fc = new JFileChooser();
            int retval = fc.showOpenDialog(table);
            if (retval == JFileChooser.APPROVE_OPTION) {
                final Path pathLoaded = fc.getSelectedFile().toPath();
                new Thread(() -> {
                    try {
                        client.addNewDistributionFile(id, pathLoaded);
                        client.sendSeedInfoToTracker();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, e1.getMessage()));
                    }
                }).start();
            }
        });
        menu.add(itemDownload);
        menu.add(itemDistribute);
        return menu;
    }

    private JPopupMenu createFilesTabPopup(int id) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemRemoveDistribute = new JMenuItem(REMOVE_DISTRIBUTE_POPUP_FILES_TAB);
        itemRemoveDistribute.addActionListener(e -> {
            new Thread(() -> {
                try {
                    client.removeDistribution(id);
                    client.sendSeedInfoToTracker();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, e1.getMessage()));
                }
            }).start();
        });
        menu.add(itemRemoveDistribute);
        return menu;
    }
}
