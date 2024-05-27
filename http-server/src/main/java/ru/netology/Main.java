package ru.netology;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        List<String> validPaths;

        // Сканируем директорию public и собираем все файлы в список
        try (var paths = Files.walk(Paths.get("./http-server/public"))) {
            validPaths = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.toString().replace("\\", "/").substring("./http-server/public".length()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Server server = new Server(9999, validPaths);
        server.start();
    }
}