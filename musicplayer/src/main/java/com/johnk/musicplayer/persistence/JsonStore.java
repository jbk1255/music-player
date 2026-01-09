package com.johnk.musicplayer.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.johnk.musicplayer.domain.Song;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.*;
import java.util.*;

public class JsonStore {

    private static final Path STORE_PATH = Paths.get(
            System.getProperty("user.home"),
            ".musicplayer",
            "data.json"
    );

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static class StoredData {
        public List<SongRecord> songs = new ArrayList<>();
        public Map<String, List<String>> playlists = new LinkedHashMap<>();
    }
    public static class SongRecord {
        public String id;
        public String title;
        public String artist;
        public String album;
        public String path;
    }

    public Optional<StoredData> load() {
        if (!Files.exists(STORE_PATH)) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(STORE_PATH)) {
            StoredData data = gson.fromJson(reader, StoredData.class);
            if (data == null) return Optional.empty();

            if (data.songs == null) data.songs = new ArrayList<>();
            if (data.playlists == null) data.playlists = new LinkedHashMap<>();

            return Optional.of(data);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void save(List<Song> songs, Map<String, List<String>> playlists) {
        try {
            Files.createDirectories(STORE_PATH.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory: " + STORE_PATH.getParent(), e);
        }

        StoredData data = new StoredData();

        if (songs != null) {
            for (Song s : songs) {
                SongRecord r = new SongRecord();
                r.id = s.getId();
                r.title = s.getTitle();
                r.artist = s.getArtist();
                r.album = s.getAlbum();
                r.path = s.getPath();
                data.songs.add(r);
            }
        }

        if (playlists != null) {
            for (Map.Entry<String, List<String>> e : playlists.entrySet()) {
                data.playlists.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }

        Path tmp = STORE_PATH.resolveSibling("data.json.tmp");
        try (Writer writer = Files.newBufferedWriter(tmp)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write storage file: " + tmp, e);
        }

        try {
            Files.move(tmp, STORE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.move(tmp, STORE_PATH, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to save storage file: " + STORE_PATH, ex);
            }
        }
    }

    public static List<Song> toSongs(StoredData data) {
        if (data == null || data.songs == null) return List.of();

        List<Song> out = new ArrayList<>();
        for (SongRecord r : data.songs) {
            if (r == null) continue;

            if (isBlank(r.id) || isBlank(r.title) || isBlank(r.artist) || isBlank(r.album) || isBlank(r.path)) {
                continue;
            }

            out.add(new Song(r.id, r.title, r.artist, r.album, r.path));
        }
        return out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
