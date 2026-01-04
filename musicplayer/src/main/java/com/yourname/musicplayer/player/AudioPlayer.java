package com.yourname.musicplayer.player;

import com.yourname.musicplayer.domain.Song;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;

public class AudioPlayer {

    private MediaPlayer mediaPlayer;
    private Song currentSong;

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
        Media media = new Media(uri);

        mediaPlayer = new MediaPlayer(media);
        currentSong = song;

        mediaPlayer.setOnError(() -> {
            RuntimeException err = (mediaPlayer != null && mediaPlayer.getError() != null)
                    ? mediaPlayer.getError()
                    : new RuntimeException("Unknown MediaPlayer error");
            stopAndDispose();
            throw err;
        });

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
        return mediaPlayer != null &&
               mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public void stopAndDispose() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } finally {
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        }
        currentSong = null;
    }

    // -------------------------
    // Progress bar support
    // -------------------------

    public boolean hasMedia() {
        return mediaPlayer != null;
    }

    public Duration getCurrentTime() {
        return (mediaPlayer == null) ? Duration.ZERO : mediaPlayer.getCurrentTime();
    }

    public Duration getTotalDuration() {
        if (mediaPlayer == null) return Duration.UNKNOWN;
        Duration d = mediaPlayer.getTotalDuration();
        return (d == null) ? Duration.UNKNOWN : d;
    }

    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return (mediaPlayer == null) ? null : mediaPlayer.currentTimeProperty();
    }

    public void seek(Duration time) {
        if (mediaPlayer == null || time == null) return;

        // MediaPlayer.seek must happen on FX thread
        if (Platform.isFxApplicationThread()) {
            mediaPlayer.seek(time);
        } else {
            Platform.runLater(() -> {
                if (mediaPlayer != null) mediaPlayer.seek(time);
            });
        }
    }

    public void setOnEndOfMedia(Runnable r) {
        if (mediaPlayer == null) return;
        mediaPlayer.setOnEndOfMedia(r);
    }

    public void setOnReady(Runnable r) {
        if (mediaPlayer == null) return;
        mediaPlayer.setOnReady(r);
    }
}
