package com.johnk.musicplayer.player;

import java.util.Collections;
import java.util.List;

import com.johnk.musicplayer.domain.Song;

public class PlaybackQueue {

    private List<Song> songs = Collections.emptyList();
    private int currentIndex = -1;

    public void setQueue(List<Song> songs) {
        this.songs = (songs == null) ? Collections.emptyList() : songs;
        currentIndex = -1;
    }

    public Song playAt(int index) {
        // allow "no selection"
        if (index < 0) {
            currentIndex = -1;
            return null;
        }
        if (index >= songs.size()) {
            return null;
        }
        currentIndex = index;
        return songs.get(currentIndex);
    }

    public Song next() {
        if (!hasNext()) return null;
        currentIndex++;
        return songs.get(currentIndex);
    }

    public Song prev() {
        if (!hasPrev()) return null;
        currentIndex--;
        return songs.get(currentIndex);
    }

    public boolean hasNext() {
        if (songs.isEmpty()) return false;
        if (currentIndex < 0) return false;
        return currentIndex + 1 < songs.size();
    }

    public boolean hasPrev() {
        if (songs.isEmpty()) return false;
        if (currentIndex < 0) return false;
        return currentIndex - 1 >= 0;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
