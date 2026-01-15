package org.example;

public class Animal {
    private int id;
    private String name;
    private String preferred_common_name;
    private String iconic_taxon_name;   // categoria/tassonomia principale
    private boolean extinct;
    private String wikipedia_url;
    private DefaultPhoto default_photo;

    public Animal(int animalId, String animalName) {
        this.id = animalId;
        this.preferred_common_name = animalName;
    }

    public int getId() {
        return id;
    }

    public String getScientificName() {
        return name;
    }

    public String getDisplayName() {
        if (preferred_common_name == null) {
            return "";
        }
        return preferred_common_name;
    }

    public String getIconicTaxon() {
        return iconic_taxon_name;
    }

    public boolean isExtinct() {
        return extinct;
    }

    public String getWikipediaUrl() {
        if (wikipedia_url == null)
            return "Wikipedia non disponibile";
        return wikipedia_url;
    }

    public String getImageUrl() {
        if (default_photo == null)
            return null;
        return default_photo.getMediumUrl();
    }
}

