package com.musicstore.domain;

/** Nature du fichier porté par un titre. */
public enum MediaType {
    AUDIO,
    VIDEO;

    public String toApi() {
        return name().toLowerCase();
    }

    public static MediaType fromApi(String value) {
        if (value != null && value.trim().equalsIgnoreCase("video")) {
            return VIDEO;
        }
        return AUDIO;
    }
}
