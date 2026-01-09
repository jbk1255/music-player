package com.johnk.musicplayer.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Playlist {

    private final String name;
    private final List<String> songIds = new ArrayList<>();

    public Playlist(String name) {
        this.name = requireNonBlank(name, "name");
    }

    public Playlist(String name, List<String> songIds) {
        this.name = requireNonBlank(name, "name");
        if (songIds != null) {
            for (String id : songIds) {
                if (id != null && !id.trim().isEmpty()) {
                    this.songIds.add(id.trim());
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public List<String> getSongIds() {
        return Collections.unmodifiableList(songIds);
    }

    public void addSongId(String songId) {
        String cleaned = requireNonBlank(songId, "songId");
        if (!songIds.contains(cleaned)) {
            songIds.add(cleaned);
        }
    }

    public void removeSongId(String songId) {
        if (songId == null) return;
        songIds.remove(songId.trim());
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Playlist)) return false;
        Playlist playlist = (Playlist) o;
        return name.equalsIgnoreCase(playlist.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
