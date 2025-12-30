package com.yourname.musicplayer.service;

import java.util.*;

public class PlaylistService {

    // Keep insertion order for nicer UI display
    private final Map<String, LinkedHashSet<String>> playlists = new LinkedHashMap<>();

    public PlaylistService() {
        // optional seed
        createPlaylist("Favorites");
        createPlaylist("Study Mix");
        createPlaylist("Gym");
    }

    public void createPlaylist(String name) {
        String safe = normalizeName(name);
        if (safe.isBlank()) {
            throw new IllegalArgumentException("Playlist name must not be blank.");
        }
        if (playlists.containsKey(safe)) {
            throw new IllegalArgumentException("Playlist already exists: " + safe);
        }
        playlists.put(safe, new LinkedHashSet<>());
    }

    public void deletePlaylist(String name) {
        String safe = normalizeName(name);
        if (!playlists.containsKey(safe)) {
            throw new IllegalArgumentException("Playlist not found: " + safe);
        }
        playlists.remove(safe);
    }

    public void addSong(String playlistName, String songId) {
        String pl = normalizeName(playlistName);
        if (!playlists.containsKey(pl)) {
            throw new IllegalArgumentException("Playlist not found: " + pl);
        }
        if (songId == null || songId.isBlank()) {
            throw new IllegalArgumentException("songId must not be blank.");
        }
        playlists.get(pl).add(songId);
    }

    public void removeSong(String playlistName, String songId) {
        String pl = normalizeName(playlistName);
        if (!playlists.containsKey(pl)) {
            throw new IllegalArgumentException("Playlist not found: " + pl);
        }
        if (songId == null || songId.isBlank()) {
            throw new IllegalArgumentException("songId must not be blank.");
        }
        playlists.get(pl).remove(songId);
    }

    public List<String> getPlaylists() {
        return Collections.unmodifiableList(new ArrayList<>(playlists.keySet()));
    }

    public List<String> getSongIds(String playlistName) {
        String pl = normalizeName(playlistName);
        LinkedHashSet<String> ids = playlists.get(pl);
        if (ids == null) {
            throw new IllegalArgumentException("Playlist not found: " + pl);
        }
        return Collections.unmodifiableList(new ArrayList<>(ids));
    }

    public boolean hasPlaylist(String playlistName) {
        return playlists.containsKey(normalizeName(playlistName));
    }

    public boolean containsSong(String playlistName, String songId) {
        String pl = normalizeName(playlistName);
        LinkedHashSet<String> ids = playlists.get(pl);
        return ids != null && ids.contains(songId);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
