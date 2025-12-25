package org.example;

public class AutocompleteAnimal {
    private int id;
    private String name;
    private String preferred_common_name;
    private String iconic_taxon_name;
    private String wikipedia_url;

    public String getScientificName() {
        return name;
    }

    public String getDisplayName() {
        if (name == null) {
            return "";
        }
        return preferred_common_name;
    }

    public String getIconicTaxon() {
        return iconic_taxon_name;
    }

    public String getWikipediaUrl() {
        if (wikipedia_url == null)
            return "Wikipedia non disponibile";
        return wikipedia_url;
    }
}

