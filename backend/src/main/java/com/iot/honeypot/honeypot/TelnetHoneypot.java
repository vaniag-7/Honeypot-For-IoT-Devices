package com.iot.honeypot.honeypot;

import com.iot.honeypot.entity.AttackLog;
import com.iot.honeypot.service.AttackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TelnetHoneypot {
    
    @Autowired
    private AttackService attackService;
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running = false;
    private final int PORT = 2323;
    
    public void start() {
        if (running) return;
        
        threadPool = Executors.newCachedThreadPool();
        threadPool.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;
                System.out.println("🤖 Telnet Honeypot started on port " + PORT);
                
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        threadPool.execute(new TelnetHandler(clientSocket));
                    } catch (SocketException e) {
                        if (running) {
                            System.out.println("❌ Telnet socket error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Failed to start Telnet honeypot: " + e.getMessage());
            }
        });
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            System.out.println("🛑 Telnet honeypot stopped");
        } catch (IOException e) {
            System.out.println("Error stopping telnet honeypot: " + e.getMessage());
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    private class TelnetHandler implements Runnable {
        private Socket clientSocket;
        
        public TelnetHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            String clientIp = clientSocket.getInetAddress().getHostAddress();
            
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                // Send fake router banner
                out.println("\r\nWelcome to TP-Link Wireless Router WR840N");
                out.println("Firmware Version: 3.16.9 Build 180529 Rel.55346n");
                out.println("Hardware Version: WR840N v5 00000000");
                out.println("===============================================");
                out.print("Login: ");
                out.flush();
                
                // Read credentials
                String username = in.readLine();
                out.print("Password: ");
                out.flush();
                String password = in.readLine();
                
                // Log the attack
                String payload = "Username: " + (username != null ? username : "null") + 
                               ", Password: " + (password != null ? password : "null");
                
                AttackLog attack = new AttackLog(clientIp, "TELNET", payload, "TP-Link Router");
                attack.setUsernameAttempt(username);
                attack.setPasswordAttempt(password);
                attackService.logAttack(attack);
                
                // Fake response and delay
                Thread.sleep(2000);
                out.println("Login failed - invalid username or password");
                out.println("Access denied - connection closed");
                
            } catch (IOException | InterruptedException e) {
                System.out.println("Client disconnected: " + clientIp);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}