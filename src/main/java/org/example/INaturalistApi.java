package org.example;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

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

    public AutocompleteAnimal autocompleteAnimal(String name) {
        String query = URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        String json = sendRequest("/taxa/autocomplete?q=" + query);
        if(json == null) {
            return null;
        }
        AutocompleteResponse response = gson.fromJson(json, AutocompleteResponse.class);

        if(response == null || response.getResults() == null) {
            return null;
        }

        for(AutocompleteAnimal animal : response.getResults()) {
            if(isAnimal(animal)) {
                return animal;
            }
        }

        return null;
    }

    private boolean isAnimal(AutocompleteAnimal a) {
        if(a.getIconicTaxon() == null) {
            return false;
        }

        return switch (a.getIconicTaxon()) {
            case "Mammalia",
                 "Aves",
                 "Reptilia",
                 "Amphibia",
                 "Actinopterygii",
                 "Insecta",
                 "Arachnida",
                 "Animalia" -> true;
            default -> false;
        };
    }
}
