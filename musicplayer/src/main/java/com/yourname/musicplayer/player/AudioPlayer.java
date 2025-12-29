package com.yourname.musicplayer.player;

import com.yourname.musicplayer.domain.Song;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class AudioPlayer {

    private MediaPlayer mediaPlayer;
    private Song currentSong;

    // UI can subscribe to playback errors (MainView will set this)
    private Consumer<String> errorListener = msg -> {};

    public void setErrorListener(Consumer<String> listener) {
        this.errorListener = (listener == null) ? (msg -> {}) : listener;
    }

    public void loadAndPlay(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("No song selected.");
        }

        Path filePath = Path.of(song.getPath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        stopAndDispose();

        String uri = filePath.toUri().toString();
        System.out.println("[AudioPlayer] Trying URI: " + uri);

        Media media = new Media(uri);

        // Media-level errors (often happen before MediaPlayer is READY)
        media.setOnError(() -> {
            MediaException err = media.getError();
            String msg = (err == null) ? "Unknown Media error" : (err.getType() + ": " + err.getMessage());
            System.err.println("[AudioPlayer] Media error: " + msg);
            errorListener.accept(msg);
        });

        mediaPlayer = new MediaPlayer(media);
        currentSong = song;

        // Player-level errors (can happen during decode/playback)
        mediaPlayer.setOnError(() -> {
            MediaException err = mediaPlayer.getError();
            String msg = (err == null) ? "Unknown MediaPlayer error" : (err.getType() + ": " + err.getMessage());
            System.err.println("[AudioPlayer] MediaPlayer error: " + msg);
            errorListener.accept(msg);
            stopAndDispose();
        });

        mediaPlayer.setOnReady(() -> {
            Duration d = media.getDuration();
            System.out.println("[AudioPlayer] READY. Duration: " + (d == null ? "?" : d.toSeconds() + "s"));
        });

        mediaPlayer.setOnPlaying(() -> System.out.println("[AudioPlayer] PLAYING"));
        mediaPlayer.setOnPaused(() -> System.out.println("[AudioPlayer] PAUSED"));
        mediaPlayer.setOnStopped(() -> System.out.println("[AudioPlayer] STOPPED"));
        mediaPlayer.setOnEndOfMedia(() -> System.out.println("[AudioPlayer] END_OF_MEDIA"));

        // Helpful status changes for debugging
        mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) ->
                System.out.println("[AudioPlayer] Status: " + oldStatus + " -> " + newStatus)
        );

        mediaPlayer.play();
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public void stopAndDispose() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (RuntimeException ignored) {
                // ignore stop errors
            } finally {
                try {
                    mediaPlayer.dispose();
                } finally {
                    mediaPlayer = null;
                }
            }
        }
        currentSong = null;
    }
}
