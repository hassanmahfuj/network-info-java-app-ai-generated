import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.io.*;


public class NetworkInfoApp extends JFrame {
    private JButton ipAddressButton, macAddressButton, pingButton, portCheckButton;
    private JTextArea outputArea;
    private JTextField ipAddressField;
    private JButton startPingButton, findPortsButton;

    public NetworkInfoApp() {
        setTitle("Network Info");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the frame on the screen

        // Create buttons
        ipAddressButton = new JButton("Get IP Address");
        macAddressButton = new JButton("Get MAC Addresses");
        pingButton = new JButton("Ping");
        portCheckButton = new JButton("Check Open Ports");

        // Create output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);

        // Create text input field for IP address
        ipAddressField = new JTextField(15);

        // Set preferred size for buttons
        ipAddressButton.setPreferredSize(new Dimension(150, 50));
        macAddressButton.setPreferredSize(new Dimension(150, 50));
        pingButton.setPreferredSize(new Dimension(150, 50));
        portCheckButton.setPreferredSize(new Dimension(150, 50));

        // Add action listeners to buttons
        ipAddressButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String ipAddress = InetAddress.getLocalHost().getHostAddress();
                    outputArea.setText("IP Address: " + ipAddress);
                } catch (Exception ex) {
                    outputArea.setText("Error getting IP address: " + ex.getMessage());
                }
            }
        });

        macAddressButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                    StringBuilder macAddresses = new StringBuilder();
                    while (networkInterfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = networkInterfaces.nextElement();
                        byte[] mac = networkInterface.getHardwareAddress();
                        if (mac != null) {
                            StringBuilder macAddr = new StringBuilder();
                            for (int i = 0; i < mac.length; i++) {
                                macAddr.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                            }
                            macAddresses.append(networkInterface.getDisplayName()).append(": ").append(macAddr.toString()).append("\n");
                        }
                    }
                    outputArea.setText("MAC Addresses:\n" + macAddresses.toString());
                } catch (Exception ex) {
                    outputArea.setText("Error getting MAC addresses: " + ex.getMessage());
                }
            }
        });

        pingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new PingFrame();
            }
        });

        portCheckButton.addActionListener(e -> new PortCheckFrame());

        // Create button panel on the left side
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.add(ipAddressButton);
        buttonPanel.add(macAddressButton);
        buttonPanel.add(pingButton);
        buttonPanel.add(portCheckButton);

        // Create output panel on the right side
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // Add panels to the frame
        getContentPane().add(buttonPanel, BorderLayout.WEST);
        getContentPane().add(outputPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    public static void main(String[] args) {
        new NetworkInfoApp();
    }
}

class PingFrame extends JFrame {
    private JTextField ipAddressField;
    private JButton startPingButton;
    private JTextArea outputArea;
    private Process pingProcess;
    private BufferedReader reader;

    public PingFrame() {
        setTitle("Ping IP Address");
        setSize(400, 300);
        setLocationRelativeTo(null); // Center the frame on the screen

        // Create text input field for IP address
        ipAddressField = new JTextField(15);

        // Create start ping button
        startPingButton = new JButton("Start Ping");
        startPingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String ip = ipAddressField.getText();
                try {
                    pingProcess = new ProcessBuilder("ping", "-n", "4", ip).start();
                    reader = new BufferedReader(new InputStreamReader(pingProcess.getInputStream()));
                    new Thread(new PingOutputReader()).start();
                } catch (IOException ex) {
                    outputArea.setText("Error while pinging: " + ex.getMessage());
                }
            }
        });

        // Create output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);

        // Create panel for text input and button
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("IP Address: "));
        inputPanel.add(ipAddressField);
        inputPanel.add(startPingButton);

        // Create panel for output area
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // Add panels to the frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(inputPanel, BorderLayout.NORTH);
        getContentPane().add(outputPanel, BorderLayout.CENTER);

        setVisible(true);

        // Handle closing of the frame
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (pingProcess != null) {
                    pingProcess.destroy();
                }
            }
        });
    }

    class PingOutputReader implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line; // Store line in a final variable
                    SwingUtilities.invokeLater(() -> outputArea.append(outputLine + "\n"));
                }
                reader.close();
            } catch (IOException ex) {
                outputArea.setText("Error reading ping output: " + ex.getMessage());
            }
        }
    }

}

class PortCheckFrame extends JFrame {
    private JTextField ipAddressField;
    private JTextField specificPortsField; // New input field for specific ports
    private JButton findPortsButton;
    private JTextArea outputArea;

    public PortCheckFrame() {
        setTitle("Check Open Ports");
        setSize(600, 300);
        setLocationRelativeTo(null); // Center the frame on the screen

        // Create text input field for IP address
        ipAddressField = new JTextField(15);

        // Create text input field for specific ports
        specificPortsField = new JTextField(15);

        // Create find ports button
        findPortsButton = new JButton("Find Ports");
        findPortsButton.addActionListener(e -> {
            String ip = ipAddressField.getText();
            String specificPortsText = specificPortsField.getText();
            outputArea.setText(""); // Clear previous output
            new Thread(() -> {
                try {
                    outputArea.append("Scanning ports on " + ip + "...\n");
                    java.util.List<Integer> specificPortsList = parseSpecificPorts(specificPortsText);
                    scanPorts(ip, specificPortsList);
                    SwingUtilities.invokeLater(() -> outputArea.append("Port scanning complete."));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> outputArea.setText("Error scanning ports: " + ex.getMessage()));
                }
            }).start();
        });

        // Create output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);

        // Create panel for text input and button
        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("IP Address: "));
        inputPanel.add(ipAddressField);
        inputPanel.add(new JLabel("Specific Ports: "));
        inputPanel.add(specificPortsField); // Add new input field for specific ports
        inputPanel.add(findPortsButton);

        // Create panel for output area
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // Add panels to the frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(inputPanel, BorderLayout.NORTH);
        getContentPane().add(outputPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private java.util.List<Integer> parseSpecificPorts(String portsText) {
        java.util.List<Integer> specificPortsList = new ArrayList<>();
        String[] portStrings = portsText.split(",");
        for (String portString : portStrings) {
            if (portString.contains("-")) {
                // Handle range of ports
                String[] range = portString.split("-");
                try {
                    int startPort = Integer.parseInt(range[0].trim());
                    int endPort = Integer.parseInt(range[1].trim());
                    for (int port = startPort; port <= endPort; port++) {
                        if (port >= 1 && port <= 65535) {
                            specificPortsList.add(port);
                        }
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                }
            } else {
                // Handle individual ports
                try {
                    int port = Integer.parseInt(portString.trim());
                    if (port >= 1 && port <= 65535) {
                        specificPortsList.add(port);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return specificPortsList;
    }


    private void scanPorts(String ip, java.util.List<Integer> specificPortsList) {
        int numThreads = Runtime.getRuntime().availableProcessors() * 2;
        int portsPerThread = 65535 / numThreads;
        java.util.List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int startPort = i * portsPerThread + 1;
            final int endPort = (i == numThreads - 1) ? 65535 : (i + 1) * portsPerThread;
            Thread thread = new Thread(() -> {
                for (int port = startPort; port <= endPort; port++) {
                    final int currentPort = port; // Create effectively final variable
                    if (specificPortsList.isEmpty() || specificPortsList.contains(currentPort)) {
                        try (Socket ignored = new Socket(ip, currentPort)) {
                            SwingUtilities.invokeLater(() -> outputArea.append("Port " + currentPort + " is open.\n"));
                        } catch (IOException ignored) {
                            SwingUtilities.invokeLater(() -> outputArea.append("Port " + currentPort + " is close.\n"));
                        }
                    }
                }
            });
            thread.start();
            threads.add(thread);
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

}
