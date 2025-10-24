// IoTHoneypotUI.java
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class IoTHoneypotUI extends JFrame implements HoneypotServer.LogListener {

    // Logs (fully-qualified to avoid ambiguity)
    private java.util.List<String> logList = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    // UI state
    private final Map<String, JLabel> deviceStatus = new ConcurrentHashMap<>(); // device -> status label
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();     // device -> last timestamp
    private final Map<String, Deque<Long>> recentHits = new ConcurrentHashMap<>(); // device -> deque of timestamps

    // thresholds (Option B)
    private final int HEARTBEAT_WINDOW_MS = 5000;   // window for heartbeat counting
    private final int HEARTBEAT_COUNT_THRESHOLD = 8; // if more than this in window -> suspicious
    private final int GENERAL_WINDOW_MS = 2000;     // general rapid-hit window
    private final int GENERAL_COUNT_THRESHOLD = 5;  // number of non-heartbeat hits in window -> suspicious

    private JPanel devicesPanel;
    private JPanel logsPanel;
    private JScrollPane logsScroll;
    private JButton startBtn, stopBtn, downloadBtn;

    private HoneypotServer server;
    private volatile boolean serverRunning = false;

    // Known devices for initial cards (UI will also add unknown devices on the fly)
    private final String[] KNOWN = {"Smart Light", "Security Camera", "Thermostat", "Door Lock", "Weather Sensor"};

    public IoTHoneypotUI() {
        setTitle("IoT Honeypot Dashboard (Cool)");
        setSize(980, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10,10));
        getContentPane().setBackground(new Color(12,15,20));

        // Title
        JLabel title = new JLabel("IoT Honeypot Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("Consolas", Font.BOLD, 22));
        title.setForeground(new Color(120,220,255));
        title.setBorder(new EmptyBorder(12,0,6,0));
        add(title, BorderLayout.NORTH);

        // Devices panel (top)
        devicesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        devicesPanel.setBackground(new Color(18,22,28));
        for (String d : KNOWN) {
            devicesPanel.add(makeDeviceCard(d));
        }
        JScrollPane devScroll = new JScrollPane(devicesPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        devScroll.setPreferredSize(new Dimension(960, 160));
        devScroll.setBorder(null);
        add(devScroll, BorderLayout.CENTER);

        // Logs panel (bottom)
        logsPanel = new JPanel();
        logsPanel.setLayout(new BoxLayout(logsPanel, BoxLayout.Y_AXIS));
        logsPanel.setBackground(new Color(10,12,16));
        logsScroll = new JScrollPane(logsPanel);
        logsScroll.setPreferredSize(new Dimension(960, 420));
        logsScroll.setBorder(new TitledBorder(new LineBorder(new Color(40,80,120)), "Live Logs", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Consolas", Font.BOLD, 12), new Color(160,220,255)));
        add(logsScroll, BorderLayout.SOUTH);

        // Controls (separate panel)
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        controls.setBackground(new Color(12,15,20));
        startBtn = styledButton("Start Server", new Color(30,170,150));
        stopBtn  = styledButton("Stop Server",  new Color(200,70,70));
        downloadBtn = styledButton("Download Logs", new Color(70,120,200));
        stopBtn.setEnabled(false);
        controls.add(startBtn);
        controls.add(stopBtn);
        controls.add(downloadBtn);
        add(controls, BorderLayout.NORTH);

        // Listeners
        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());
        downloadBtn.addActionListener(e -> saveLogs());

        // Timer for timeouts
        new javax.swing.Timer(2000, e -> checkTimeouts()).start();

        setVisible(true);
        appendLog("UI ready. Click Start Server to begin listening on port 8080.", true);
    }

    // create a styled device card and register its status label
    private JPanel makeDeviceCard(String deviceName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(170, 90));
        panel.setBackground(new Color(22,30,40));
        panel.setBorder(new CompoundBorder(new LineBorder(new Color(60,70,80),1,true), new EmptyBorder(6,6,6,6)));

        JLabel name = new JLabel(deviceName, SwingConstants.CENTER);
        name.setFont(new Font("Consolas", Font.BOLD, 14));
        name.setForeground(new Color(160,220,255));

        JLabel status = new JLabel("Offline", SwingConstants.CENTER);
        status.setFont(new Font("Consolas", Font.PLAIN, 13));
        status.setForeground(Color.RED);

        JLabel last = new JLabel("", SwingConstants.CENTER);
        last.setFont(new Font("Consolas", Font.PLAIN, 11));
        last.setForeground(new Color(200,200,200));
        last.setText("");

        panel.add(name, BorderLayout.NORTH);
        panel.add(status, BorderLayout.CENTER);
        panel.add(last, BorderLayout.SOUTH);

        deviceStatus.put(deviceName, status);
        // store last label as client property for updates
        panel.putClientProperty("lastSeenLabel", last);
        return panel;
    }

    private JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(160,36));
        b.setFont(new Font("Consolas", Font.BOLD, 13));
        return b;
    }

    // Start server
    private void startServer() {
        if (serverRunning) {
            appendLog("Server is already running.", true);
            return;
        }
        try {
            server = new HoneypotServer(8080, this);
            server.start();
            serverRunning = true;
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            appendLog("Honeypot server started on port 8080", true);
        } catch (Exception ex) {
            appendLog("Failed to start server: " + ex.getMessage(), false);
            ex.printStackTrace();
        }
    }

    // Stop server
    private void stopServer() {
        if (!serverRunning || server == null) {
            appendLog("Server not running.", false);
            return;
        }
        server.stop();
        serverRunning = false;
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        appendLog("Honeypot server stopped.", true);
    }

    // Save logs
    private void saveLogs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("honeypot_logs.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(f)) {
                for (String s : logList) pw.println(s);
                appendLog("Logs saved to " + f.getAbsolutePath(), true);
            } catch (IOException ex) {
                appendLog("Failed to save logs: " + ex.getMessage(), false);
            }
        }
    }

    // HoneypotServer callback
    @Override
    public void onLogReceived(String srcIp, String device, String proto, String action, String details) {
        SwingUtilities.invokeLater(() -> {
            // sliding window detection (Option B)
            boolean unusual = detectUnusualBehavior(device, details);

            // update last seen and device status
            lastSeen.put(device, System.currentTimeMillis());
            updateDeviceCard(device, unusual);

            String summary = String.format("From %s | device=%s | %s", srcIp, device, details);
            appendLog(unusual ? ("SUSPICIOUS - " + summary) : summary, !unusual);

            // if unusual then trigger red glow animation
            if (unusual) triggerRedGlow(device);
        });
    }

    // Option B: sliding-window detector
    private boolean detectUnusualBehavior(String device, String details) {
        String lower = details.toLowerCase();

        // immediate keyword detection
        if (lower.contains("scan") || lower.contains("attack") || lower.contains("bruteforce")
                || lower.contains("exploit") || lower.contains("unauthorized")) {
            // still record hit
            Deque<Long> dq = recentHits.computeIfAbsent(device, k -> new ArrayDeque<>());
            dq.addLast(System.currentTimeMillis());
            return true;
        }

        long now = System.currentTimeMillis();
        Deque<Long> dq = recentHits.computeIfAbsent(device, k -> new ArrayDeque<>());
        dq.addLast(now);

        // purge old timestamps older than the larger window
        while (!dq.isEmpty() && now - dq.peekFirst() > Math.max(HEARTBEAT_WINDOW_MS, GENERAL_WINDOW_MS)) {
            dq.removeFirst();
        }

        boolean isHeartbeat = lower.contains("heartbeat") || lower.contains("ping");

        // count heartbeats in heartbeat window
        int hbCount = 0;
        for (Long t : dq) if (now - t <= HEARTBEAT_WINDOW_MS) hbCount++;

        if (isHeartbeat) {
            if (hbCount > HEARTBEAT_COUNT_THRESHOLD) return true; // too many heartbeats -> suspicious
            return false; // otherwise benign
        }

        // count recent non-heartbeat hits in general window
        int recentCount = 0;
        for (Long t : dq) if (now - t <= GENERAL_WINDOW_MS) recentCount++;

        if (recentCount >= GENERAL_COUNT_THRESHOLD) {
            return true;
        }

        return false;
    }

    // update device card UI (status text & last seen)
    private void updateDeviceCard(String device, boolean unusual) {
        JLabel status = deviceStatus.get(device);
        if (status == null) {
            // dynamically create card for unknown device
            JPanel newCard = makeDeviceCard(device);
            devicesPanel.add(newCard);
            devicesPanel.revalidate();
            status = deviceStatus.get(device);
        }

        if (unusual) {
            status.setText("⚠ Unusual");
            status.setForeground(Color.ORANGE);
        } else {
            status.setText("Active");
            status.setForeground(new Color(0,220,120));
        }

        // find the card panel and update its lastSeen sublabel
        for (Component comp : devicesPanel.getComponents()) {
            if (!(comp instanceof JPanel)) continue;
            JPanel p = (JPanel) comp;
            Component[] ch = p.getComponents();
            if (ch.length > 0 && ch[0] instanceof JLabel) {
                JLabel name = (JLabel) ch[0];
                if (device.equals(name.getText())) {
                    JLabel lastSeenLabel = (JLabel) p.getClientProperty("lastSeenLabel");
                    if (lastSeenLabel == null) {
                        // fallback: maybe third child is the last label
                        if (ch.length > 2 && ch[2] instanceof JLabel) lastSeenLabel = (JLabel) ch[2];
                    }
                    if (lastSeenLabel != null) {
                        lastSeenLabel.setText("Last: " + sdf.format(new Date()));
                    }
                    break;
                }
            }
        }
    }

    // red-glow animation: toggles the card border color for ~3 seconds
    private void triggerRedGlow(String device) {
        // find the card JPanel for this device
        JPanel found = null;
        for (Component comp : devicesPanel.getComponents()) {
            if (!(comp instanceof JPanel)) continue;
            JPanel p = (JPanel) comp;
            Component[] ch = p.getComponents();
            if (ch.length > 0 && ch[0] instanceof JLabel) {
                JLabel name = (JLabel) ch[0];
                if (device.equals(name.getText())) {
                    found = p;
                    break;
                }
            }
        }
        if (found == null) return;

        final JPanel card = found;
        final Border original = card.getBorder();
        final Color glowColor = new Color(255, 80, 80);

        // animation: toggle border color every 300ms for 3 seconds
        final int[] ticks = {0};
        javax.swing.Timer t = new javax.swing.Timer(300, null);
        t.addActionListener(evt -> {
            ticks[0]++;
            if (ticks[0] % 2 == 1) {
                card.setBorder(new CompoundBorder(new LineBorder(glowColor, 3, true), new EmptyBorder(6,6,6,6)));
            } else {
                card.setBorder(new CompoundBorder(new LineBorder(new Color(60,70,80), 1, true), new EmptyBorder(6,6,6,6)));
            }
            if (ticks[0] >= 10) { // ~3s
                t.stop();
                card.setBorder(original);
            }
        });
        t.setInitialDelay(0);
        t.start();
    }

    // append log row to UI
    private void appendLog(String text, boolean ok) {
        String line = "[" + sdf.format(new Date()) + "] " + text;
        logList.add(line);
        SwingUtilities.invokeLater(() -> {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(new Color(18,22,28));
            row.setBorder(new CompoundBorder(new EmptyBorder(6,6,6,6),
                    new LineBorder(ok ? new Color(30,160,80) : new Color(200,80,80), 1, true)));
            JLabel l = new JLabel(line);
            l.setForeground(ok ? new Color(190,255,200) : new Color(255,200,200));
            l.setFont(new Font("Consolas", Font.PLAIN, 12));
            row.add(l, BorderLayout.CENTER);
            logsPanel.add(row, 0); // newest on top
            logsPanel.revalidate();
            logsPanel.repaint();
            // scroll to top (newest)
            SwingUtilities.invokeLater(() -> logsScroll.getVerticalScrollBar().setValue(0));
        });
    }

    private void appendLog(String text) { appendLog(text, true); }

    // check timeouts and mark offline after inactivity (10s)
    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, JLabel> e : deviceStatus.entrySet()) {
            String device = e.getKey();
            Long last = lastSeen.get(device);
            if (last == null || (now - last) > 10000) {
                e.getValue().setText("Offline");
                e.getValue().setForeground(Color.RED);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IoTHoneypotUI());
    }
}
