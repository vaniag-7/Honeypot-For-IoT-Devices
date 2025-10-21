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
public class HttpHoneypot {
    
    @Autowired
    private AttackService attackService;
    
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running = false;
    private final int PORT = 8081;
    
    public void start() {
        if (running) return;
        
        threadPool = Executors.newCachedThreadPool();
        threadPool.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;
                System.out.println("🌐 HTTP Honeypot started on port " + PORT);
                
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        threadPool.execute(new HttpHandler(clientSocket));
                    } catch (SocketException e) {
                        if (running) {
                            System.out.println("❌ HTTP socket error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Failed to start HTTP honeypot: " + e.getMessage());
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
            System.out.println("🛑 HTTP honeypot stopped");
        } catch (IOException e) {
            System.out.println("Error stopping HTTP honeypot: " + e.getMessage());
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    private class HttpHandler implements Runnable {
        private Socket clientSocket;
        
        public HttpHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            String clientIp = clientSocket.getInetAddress().getHostAddress();
            
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                // Read HTTP request
                String requestLine = in.readLine();
                if (requestLine == null) return;
                
                String[] requestParts = requestLine.split(" ");
                String method = requestParts.length > 0 ? requestParts[0] : "UNKNOWN";
                String path = requestParts.length > 1 ? requestParts[1] : "/";
                
                // Read headers to find User-Agent
                String userAgent = "Unknown";
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("user-agent:")) {
                        userAgent = line.substring(11).trim();
                    }
                }
                
                // Log the attack
                String payload = method + " " + path + " | User-Agent: " + userAgent;
                AttackLog attack = new AttackLog(clientIp, "HTTP", payload, "D-Link Camera");
                attackService.logAttack(attack);
                
                // Send fake HTTP response
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println("Connection: close");
                out.println();
                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>D-Link Camera</title></head>");
                out.println("<body>");
                out.println("<h1>D-Link Wireless Camera DCS-932L</h1>");
                out.println("<p>Please <a href='/login.html'>login</a> to access camera controls.</p>");
                out.println("</body></html>");
                
            } catch (IOException e) {
                System.out.println("HTTP Client error: " + clientIp);
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