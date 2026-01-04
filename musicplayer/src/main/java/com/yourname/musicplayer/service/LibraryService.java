package com.yourname.musicplayer.service;

import com.yourname.musicplayer.domain.Song;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class LibraryService {

    private final Map<String, Song> songsById = new HashMap<>();
    private final List<String> songOrder = new ArrayList<>();

    private final Map<String, List<String>> artistToSongIds = new HashMap<>();
    private final Map<String, List<String>> albumToSongIds = new HashMap<>();

    private final Map<String, String> pathToSongId = new HashMap<>();

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("mp3", "wav", "m4a");

    public void importFolder(Path folder) {
        validateFolder(folder);

        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedAudioFile)
                    .sorted()
                    .forEach(this::addFileAsSongIfMissing);
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan folder: " + folder, e);
        }
    }

    public void clearLibrary() {
        clear();
    }

    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        for (String id : songOrder) {
            Song s = songsById.get(id);
            if (s != null) songs.add(s);
        }
        return Collections.unmodifiableList(songs);
    }

    public Optional<Song> getSongById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(songsById.get(id.trim()));
    }

    public void loadLibrary(List<Song> songs) {
        clear();

        if (songs == null) return;

        for (Song s : songs) {
            if (s == null) continue;

            songsById.put(s.getId(), s);
            songOrder.add(s.getId());

            pathToSongId.put(s.getPath(), s.getId());

            indexAppend(artistToSongIds, s.getArtist(), s.getId());
            indexAppend(albumToSongIds, s.getAlbum(), s.getId());
        }
    }

    public List<Song> resolveSongsByIds(List<String> songIds) {
        if (songIds == null) return List.of();
        List<Song> out = new ArrayList<>();
        for (String id : songIds) {
            Song s = (id == null) ? null : songsById.get(id);
            if (s != null) out.add(s);
        }
        return Collections.unmodifiableList(out);
    }

    public Map<String, List<String>> getArtistToSongIds() {
        return unmodifiableCopyMapOfLists(artistToSongIds);
    }

    public Map<String, List<String>> getAlbumToSongIds() {
        return unmodifiableCopyMapOfLists(albumToSongIds);
    }

    private void clear() {
        songsById.clear();
        songOrder.clear();
        artistToSongIds.clear();
        albumToSongIds.clear();
        pathToSongId.clear();
    }

    private void validateFolder(Path folder) {
        if (folder == null) throw new IllegalArgumentException("folder must not be null");
        if (!Files.exists(folder)) throw new IllegalArgumentException("folder does not exist: " + folder);
        if (!Files.isDirectory(folder)) throw new IllegalArgumentException("path is not a directory: " + folder);
    }

    private boolean isSupportedAudioFile(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return false;
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    private void addFileAsSongIfMissing(Path file) {
        String path = file.toAbsolutePath().toString();

        if (pathToSongId.containsKey(path)) return;

        String filename = file.getFileName().toString();
        String title = stripExtension(filename);

        String artist = "Unknown Artist";
        String album = "Unknown Album";

        String stableId = UUID.nameUUIDFromBytes(path.getBytes(StandardCharsets.UTF_8)).toString();

        Song song = new Song(stableId, title, artist, album, path);

        songsById.put(song.getId(), song);
        songOrder.add(song.getId());
        pathToSongId.put(path, song.getId());

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
}
