package com.yourname.musicplayer.service;

import com.yourname.musicplayer.domain.Song;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class LibraryService {

    private final Map<String, Song> songsById = new HashMap<>();
    private final List<String> songOrder = new ArrayList<>(); // preserves display order

    private final Map<String, List<String>> artistToSongIds = new HashMap<>();
    private final Map<String, List<String>> albumToSongIds = new HashMap<>();

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("mp3", "wav", "m4a");

    public void importFolder(Path folder) {
        validateFolder(folder);

        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedAudioFile)
                    .sorted()
                    .forEach(this::addFileAsSong);
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan folder: " + folder, e);
        }
    }

    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        for (String id : songOrder) {
            Song s = songsById.get(id);
            if (s != null) {
                songs.add(s);
            }
        }
        return Collections.unmodifiableList(songs);
    }

    // ✅ Merge 7: simple search
    public List<Song> search(String query) {
        if (query == null || query.isBlank()) {
            return getAllSongs();
        }

        String q = query.trim().toLowerCase(Locale.ROOT);

        List<Song> results = new ArrayList<>();
        for (Song s : getAllSongs()) {
            String title = safeLower(s.getTitle());
            String artist = safeLower(s.getArtist());
            String album = safeLower(s.getAlbum());

            if (title.contains(q) || artist.contains(q) || album.contains(q)) {
                results.add(s);
            }
        }
        return results;
    }

    public Optional<Song> getSongById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(songsById.get(id.trim()));
    }

    public Map<String, List<String>> getArtistToSongIds() {
        return unmodifiableCopyMapOfLists(artistToSongIds);
    }

    public Map<String, List<String>> getAlbumToSongIds() {
        return unmodifiableCopyMapOfLists(albumToSongIds);
    }

    private void validateFolder(Path folder) {
        if (folder == null) {
            throw new IllegalArgumentException("folder must not be null");
        }
        if (!Files.exists(folder)) {
            throw new IllegalArgumentException("folder does not exist: " + folder);
        }
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("path is not a directory: " + folder);
        }
    }

    private boolean isSupportedAudioFile(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return false;

        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    private void addFileAsSong(Path file) {
        String filename = file.getFileName().toString();
        String title = stripExtension(filename);

        String artist = "Unknown Artist";
        String album = "Unknown Album";
        String path = file.toAbsolutePath().toString();

        Song song = new Song(title, artist, album, path);

        songsById.put(song.getId(), song);
        songOrder.add(song.getId());

        indexAppend(artistToSongIds, song.getArtist(), song.getId());
        indexAppend(albumToSongIds, song.getAlbum(), song.getId());
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }

    private void indexAppend(Map<String, List<String>> index, String key, String songId) {
        String safeKey = (key == null || key.isBlank()) ? "Unknown" : key.trim();
        index.computeIfAbsent(safeKey, k -> new ArrayList<>()).add(songId);
    }

    private Map<String, List<String>> unmodifiableCopyMapOfLists(Map<String, List<String>> src) {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> e : src.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    private String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase(Locale.ROOT);
    }
}
