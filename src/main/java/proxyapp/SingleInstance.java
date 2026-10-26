package proxyapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SingleInstance {

    public static final int PORT = 47653;

    private final int port;
    private ServerSocket server;
    private volatile Runnable onShow;
    private volatile boolean pendingShow;

    public SingleInstance(int port) {
        this.port = port;
    }

    public synchronized boolean tryLock() {
        try {
            server = new ServerSocket(port, 5, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            askOtherInstanceToShow();
            return false;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                listen();
            }
        }, "single-instance");
        t.setDaemon(true);
        t.start();
        return true;
    }

    public void setOnShow(Runnable action) {
        onShow = action;
        if (pendingShow && action != null) {
            pendingShow = false;
            action.run();
        }
    }

    public synchronized void close() {
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
            }
            server = null;
        }
    }

    private void listen() {
        ServerSocket s = server;
        while (s != null && !s.isClosed()) {
            try {
                Socket c = s.accept();
                try {
                    c.setSoTimeout(2000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    if ("SHOW".equals(r.readLine())) {
                        Runnable action = onShow;
                        if (action == null) {
                            pendingShow = true;
                        } else {
                            action.run();
                        }
                    }
                } finally {
                    c.close();
                }
            } catch (IOException e) {
                if (s.isClosed()) {
                    return;
                }
            }
        }
    }

    private void askOtherInstanceToShow() {
        try (Socket c = new Socket(InetAddress.getLoopbackAddress(), port)) {
            OutputStream out = c.getOutputStream();
            out.write("SHOW\n".getBytes("US-ASCII"));
            out.flush();
        } catch (IOException e) {
        }
    }
}
