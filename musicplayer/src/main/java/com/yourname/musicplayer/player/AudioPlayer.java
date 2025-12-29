package com.yourname.musicplayer.player;

import com.yourname.musicplayer.domain.Song;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

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
            stopAndDispose();
            throw mediaPlayer.getError();
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
}
