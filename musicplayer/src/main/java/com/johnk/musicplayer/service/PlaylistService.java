package com.johnk.musicplayer.service;

import java.util.*;

public class PlaylistService {

    private final Map<String, LinkedHashSet<String>> playlists = new LinkedHashMap<>();

    public PlaylistService() {
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

    public Map<String, List<String>> exportPlaylists() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> e : playlists.entrySet()) {
            out.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return out;
    }

    public void loadPlaylists(Map<String, List<String>> data) {
        playlists.clear();

        if (data == null) return;

        for (Map.Entry<String, List<String>> e : data.entrySet()) {
            String name = normalizeName(e.getKey());
            if (name.isBlank()) continue;

            LinkedHashSet<String> ids = new LinkedHashSet<>();
            if (e.getValue() != null) {
                for (String id : e.getValue()) {
                    if (id != null && !id.isBlank()) {
                        ids.add(id.trim());
                    }
                }
            }

            playlists.put(name, ids);
        }
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
