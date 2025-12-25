package org.example;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class INaturalistApi {
    private static final String base_url = "https://api.inaturalist.org/v1";

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    private String sendRequest(String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Accept", "application/json")
                    .uri(URI.create(base_url + endpoint))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            } else {
                System.out.println("Errore API iNaturalist" + status + ": " + response.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Errore nella richiesta API iNaturalist: " + e.getMessage());
            return null;
        }
    }

    public AutocompleteResponse autocompleteAnimal(String name) {
        String query = name.trim().replace(" ", "%20");
        String json = sendRequest("/taxa/autocomplete?q=" + query);
        return gson.fromJson(json, AutocompleteResponse.class);
    }
}
