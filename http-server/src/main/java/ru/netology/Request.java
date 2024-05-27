package ru.netology;


import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> queryParams;

    public Request(String requestLine) throws URISyntaxException, IllegalArgumentException {
        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Частей не равно 3 (метод, путь, версия HTTP): " + requestLine);
        }

        this.method = parts[0];
        URI uri = new URI(parts[1]);
        this.path = uri.getPath();
        this.queryParams = URLEncodedUtils.parse(uri, "UTF-8")
                .stream()
                .collect(Collectors.toMap(
                        NameValuePair::getName,
                        NameValuePair::getValue
                ));
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }
}
