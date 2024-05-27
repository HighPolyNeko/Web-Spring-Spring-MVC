package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

class Server {
  // Константы для HTTP-ответов
  private static final String RESPONSE_404 = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
  private static final String CONNECTION_CLOSE = "Connection: close\r\n\r\n";
  private final int port;
  private final List<String> validPaths;
  private final ExecutorService threadPool;

  public Server(int port, List<String> validPaths) {
    this.port = port;
    this.validPaths = List.copyOf(validPaths);
    this.threadPool = Executors.newFixedThreadPool(64);
  }

  // Запуск сервера
  public void start() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      while (true) {
        // Принимаем входящее соединение
        Socket socket = serverSocket.accept();
        // Обрабатываем соединение в отдельном потоке
        threadPool.submit(() -> handleConnection(socket));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Обработка входящих соединений
  private void handleConnection(Socket socket) {
    try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
    ) {
      String requestLine = in.readLine();
      if (requestLine == null || !requestLine.startsWith("GET")) {
        // Если запрос пустой или не начинается с "GET", возвращаем 404
        sendResponse(out, RESPONSE_404.getBytes());
        return;
      }

      String[] parts = requestLine.split(" ");
      if (parts.length != 3) {
        // Если количество частей не равно 3 (метод, путь, версия HTTP), возвращаем 404
        sendResponse(out, RESPONSE_404.getBytes());
        return;
      }

      // Получаем путь
      String path = parts[1];
      // Проверка на валидность пути
      if (!validPaths.contains(path)) {
        sendResponse(out, RESPONSE_404.getBytes());
        return;
      }

      // Путь к файлу и MIME-тип
      Path filePath = Paths.get("./http-server/public", path);
      String mimeType = Files.probeContentType(filePath);

      if (path.equals("/classic.html")) {
        // Специальный случай для classic.html
        String template = Files.readString(filePath);
        String content = template.replace("{time}", LocalDateTime.now().toString());
        sendResponse(out, buildHttpResponse("200 OK", mimeType, content.getBytes()));
      } else {
        // Ответ пользователю
        byte[] content = Files.readAllBytes(filePath);
        sendResponse(out, buildHttpResponse("200 OK", mimeType, content));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      // Гарантированно закрываем сокет
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // Метод для отправки HTTP-ответа клиенту
  private void sendResponse(BufferedOutputStream out, byte[] response) throws IOException {
    out.write(response);
    out.flush();
  }

  // Метод для построения HTTP-ответа
  private byte[] buildHttpResponse(String status, String mimeType, byte[] content) {
    // Формируем заголовки ответа
    String headers = "HTTP/1.1 " + status + "\r\n" +
            "Content-Type: " + mimeType + "\r\n" +
            "Content-Length: " + content.length + "\r\n" +
            CONNECTION_CLOSE;
    byte[] headerBytes = headers.getBytes();
    // Объединяем заголовки и контент в один массив байтов
    byte[] response = new byte[headerBytes.length + content.length];
    System.arraycopy(headerBytes, 0, response, 0, headerBytes.length);
    System.arraycopy(content, 0, response, headerBytes.length, content.length);
    return response;
  }
}
