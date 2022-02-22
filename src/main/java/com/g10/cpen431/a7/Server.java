package com.g10.cpen431.a7;

import com.g10.util.SystemUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Program started.");
        System.out.println("Max Memory: " + Runtime.getRuntime().maxMemory());

        Configurator.setRootLevel(Level.WARN);

        HashtableStorage dataModel = new HashtableStorage();
        KeyValueStorageServer server = new KeyValueStorageServer(dataModel);

        SystemUtil.init(); // FIXME: Important: Put it here because GCs should run after cache clean-ups

        int n_threads = SystemUtil.concurrencyLevel();
        System.out.println(n_threads + " threads.");

        Thread[] threads = new Thread[n_threads];
        for (int i = 0; i < n_threads; i++) {
            threads[i] = new Thread(() -> run(server));
            threads[i].start();
        }
        for (int i = 0; i < n_threads; i++) {
            threads[i].join();
        }
    }

    private static void run(KeyValueStorageServer server) {
        try {
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
