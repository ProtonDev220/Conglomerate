package org.joshp.conglomerate;

import com.sun.net.httpserver.HttpServer;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public final class Conglomerate extends JavaPlugin {

    public static String combinePaths(String path1, String path2) {
        return path1.replace(System.getProperty("file.separator"), "") + path2;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        File packSources = new File(getDataFolder(), "resourcepacks");
        packSources.mkdir();
        File unzipDestination = new File(getDataFolder(), "sources");
        unzipDestination.mkdir();
        File outputDestination = new File(getDataFolder(), "serverpacks");
        outputDestination.mkdir();

        if (Objects.requireNonNull(new File(getDataFolder().getAbsolutePath() + System.getProperty("file.separator") + "serverpacks").listFiles()).length > 0) {
            if (new File(Objects.requireNonNull(new File(getDataFolder().getAbsolutePath() + System.getProperty("file.separator") + "serverpacks").listFiles())[0].getAbsolutePath()).exists()) {
                new File(Objects.requireNonNull(new File(getDataFolder().getAbsolutePath() + System.getProperty("file.separator") + "serverpacks").listFiles())[0].getAbsolutePath()).delete();
                getLogger().info("Deleted previously created server resourcepack");
            }
        }

        FileWriter credits = null;
        try {
            credits = new FileWriter(getDataFolder().getAbsolutePath() + System.getProperty("file.separator") + "sources" + System.getProperty("file.separator") + "includedpacks.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int i = 0;
        while (i < Objects.requireNonNull(packSources.listFiles()).length) {

            try {
                new ZipFile(Objects.requireNonNull(packSources.listFiles())[i]).extractAll(unzipDestination.getPath());
            } catch (ZipException e) {
                throw new RuntimeException(e);
            }

            try {
                credits.append(Objects.requireNonNull(packSources.listFiles())[i].getName()).append("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            getLogger().info("Successfully applied " + Objects.requireNonNull(packSources.listFiles())[i].getName() + " to server resources");

            i++;
        }

        try {
            credits.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            ZipFile resourcesOut = new ZipFile(getDataFolder().getAbsolutePath() + System.getProperty("file.separator") + "serverpacks" + System.getProperty("file.separator") + "Conglomerate" + String.valueOf(UUID.randomUUID()) + ".zip");
            for (int j = 0; j < Objects.requireNonNull(unzipDestination.list()).length; j++) {
                if (Objects.requireNonNull(unzipDestination.listFiles())[j].isDirectory()) {
                    resourcesOut.addFolder(Objects.requireNonNull(unzipDestination.listFiles())[j]);
                } else {
                    resourcesOut.addFile(Objects.requireNonNull(unzipDestination.listFiles())[j]);
                }
            }
        } catch (ZipException e) {
            throw new RuntimeException(e);
        }

        try {
            Files
                    .walk(Paths.get(unzipDestination.getPath()))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(getConfig().getInt("port")), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.createContext("/file", new MyHandler());
        server.setExecutor(null);
        server.start();

        getLogger().info("Resourcepack server has started on port " + getConfig().getInt("port"));
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String filePath = Objects.requireNonNull(new File(getPlugin(Conglomerate.class).getDataFolder().getPath() + System.getProperty("file.separator") + "serverpacks").listFiles())[0].getAbsolutePath(); // Change this to your file's path
            FileInputStream fileInputStream = new FileInputStream(filePath);

            byte[] data = fileInputStream.readAllBytes();

            exchange.sendResponseHeaders(200, data.length);
            OutputStream os = exchange.getResponseBody();
            os.write(data);
            os.close();
            fileInputStream.close();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
