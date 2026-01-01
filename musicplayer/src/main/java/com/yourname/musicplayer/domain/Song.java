package com.yourname.musicplayer.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public final class Song {

    private final String id;
    private final String title;
    private final String artist;
    private final String album;
    private final String path;

    /**
     * Creates a Song with a STABLE id derived from its file path.
     * This prevents playlist songIds from breaking when you re-import.
     */
    public Song(String title, String artist, String album, String path) {
        this(stableIdFromPath(path), title, artist, album, path);
    }

    public Song(String id, String title, String artist, String album, String path) {
        this.id = requireNonBlank(id, "id");
        this.title = requireNonBlank(title, "title");
        this.artist = requireNonBlank(artist, "artist");
        this.album = requireNonBlank(album, "album");
        this.path = requireNonBlank(path, "path");
    }

    /**
     * Stable UUID based on the absolute path (lowercased).
     * If you import the same file again, it gets the same id.
     */
    private static String stableIdFromPath(String path) {
        String safe = (path == null) ? "" : path.trim().toLowerCase();
        return UUID.nameUUIDFromBytes(safe.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return title + " — " + artist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Song)) return false;
        Song song = (Song) o;
        return id.equals(song.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
