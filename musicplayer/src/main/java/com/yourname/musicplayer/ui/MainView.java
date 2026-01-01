package com.yourname.musicplayer.ui;

import com.yourname.musicplayer.domain.Song;
import com.yourname.musicplayer.persistence.JsonStore;
import com.yourname.musicplayer.player.AudioPlayer;
import com.yourname.musicplayer.player.PlaybackQueue;
import com.yourname.musicplayer.service.LibraryService;
import com.yourname.musicplayer.service.PlaylistService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainView {

    private final BorderPane root = new BorderPane();

    private final TextField searchField = new TextField();
    private final Button importFolderButton = new Button("Import Folder");

    private final ListView<Song> songsListView = new ListView<>();
    private final ListView<String> playlistsListView = new ListView<>();

    private final Button prevButton = new Button("Prev");
    private final Button playPauseButton = new Button("Play");
    private final Button nextButton = new Button("Next");

    private final Label selectedLabel = new Label("Selected: —");
    private final Label nowPlayingLabel = new Label("Now Playing: —");
    private final Label statusLabel = new Label("");

    private final LibraryService libraryService = new LibraryService();
    private final PlaylistService playlistService = new PlaylistService();

    private final ObservableList<Song> displayedSongs = FXCollections.observableArrayList();
    private List<Song> librarySongsSnapshot = List.of();

    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final PlaybackQueue playbackQueue = new PlaybackQueue();

    private Song selectedSong = null;

    private boolean showingPlaylist = false;
    private String activePlaylistName = null;

    private final Button newPlaylistButton = new Button("New Playlist");
    private final Button addToPlaylistButton = new Button("Add to Playlist");
    private final Button removeFromPlaylistButton = new Button("Remove Song");

    private final JsonStore jsonStore = new JsonStore();

    public MainView() {
        buildLayout();
        configureListRendering();
        wireHandlers();

        loadState();

        refreshPlaylistsList();
        refreshPlaylistButtons();
    }

    public Parent getRoot() {
        return root;
    }

    public void saveState() {
        List<Song> songsToSave = libraryService.getAllSongs();
        Map<String, List<String>> playlistsToSave = playlistService.exportPlaylists();
        jsonStore.save(songsToSave, playlistsToSave);
    }

    private void loadState() {
        Optional<JsonStore.StoredData> maybe = jsonStore.load();
        if (maybe.isEmpty()) {
            librarySongsSnapshot = List.of();
            displayedSongs.setAll(librarySongsSnapshot);
            playbackQueue.setQueue(displayedSongs);
            return;
        }

        JsonStore.StoredData data = maybe.get();

        List<Song> songs = JsonStore.toSongs(data);
        libraryService.loadLibrary(songs);

        playlistService.loadPlaylists(data.playlists);

        librarySongsSnapshot = libraryService.getAllSongs();
        displayedSongs.setAll(librarySongsSnapshot);
        playbackQueue.setQueue(displayedSongs);

        statusLabel.setText(
                librarySongsSnapshot.isEmpty()
                        ? ""
                        : "Loaded " + librarySongsSnapshot.size() + " song(s) from saved library."
        );
    }

    private void buildLayout() {
        root.setPadding(new Insets(12));

        Label title = new Label("Music Player");
        title.getStyleClass().add("title");

        searchField.setPromptText("Search songs, artists, albums...");
        searchField.setMinWidth(260);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, title, spacer, searchField, importFolderButton);
        topBar.setPadding(new Insets(0, 0, 10, 0));
        root.setTop(topBar);

        VBox songsPane = new VBox(6, sectionHeader("Library"), statusLabel, songsListView);
        songsPane.setPadding(new Insets(0, 10, 0, 0));
        VBox.setVgrow(songsListView, Priority.ALWAYS);

        HBox playlistActions = new HBox(8, newPlaylistButton, addToPlaylistButton, removeFromPlaylistButton);
        addToPlaylistButton.setDisable(true);
        removeFromPlaylistButton.setDisable(true);

        VBox playlistsPane = new VBox(
                6,
                sectionHeader("Playlists"),
                playlistsListView,
                playlistActions
        );
        playlistsPane.setPrefWidth(280);
        VBox.setVgrow(playlistsListView, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(songsPane, playlistsPane);
        splitPane.setDividerPositions(0.7);
        root.setCenter(splitPane);

        prevButton.setTooltip(new Tooltip("Previous"));
        playPauseButton.setTooltip(new Tooltip("Play / Pause"));
        nextButton.setTooltip(new Tooltip("Next"));

        HBox controls = new HBox(10, prevButton, playPauseButton, nextButton);
        controls.setPadding(new Insets(10, 0, 0, 0));

        nowPlayingLabel.setVisible(false);
        nowPlayingLabel.setManaged(false);

        VBox rightLabels = new VBox(2, selectedLabel, nowPlayingLabel);

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(12, controls, bottomSpacer, rightLabels);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(bottomBar);

        prevButton.setDisable(true);
        playPauseButton.setDisable(true);
        nextButton.setDisable(true);

        songsListView.setItems(displayedSongs);

        statusLabel.setWrapText(true);
        statusLabel.setMinHeight(18);
    }

    private Label sectionHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-header");
        return label;
    }

    private void configureListRendering() {
        songsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                setText(empty || song == null ? null : song.toString());
            }
        });
    }

    private void wireHandlers() {

        songsListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx == null || newIdx.intValue() < 0) {
                selectedSong = null;
                playbackQueue.playAt(-1);

                selectedLabel.setText("Selected: —");
                updateNowPlayingHint();

                playPauseButton.setDisable(true);
                prevButton.setDisable(true);
                nextButton.setDisable(true);
                playPauseButton.setText("Play");

                refreshPlaylistButtons();
                return;
            }

            int idx = newIdx.intValue();
            selectedSong = displayedSongs.get(idx);
            playbackQueue.playAt(idx);

            selectedLabel.setText("Selected: " + selectedSong);
            updateNowPlayingHint();

            playPauseButton.setDisable(false);
            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());

            if (audioPlayer.getCurrentSong() != null
                    && selectedSong.equals(audioPlayer.getCurrentSong())
                    && audioPlayer.isPlaying()) {
                playPauseButton.setText("Pause");
            } else {
                playPauseButton.setText("Play");
            }

            refreshPlaylistButtons();
        });

        importFolderButton.setOnAction(e -> handleImportFolder());
        playPauseButton.setOnAction(e -> handlePlayPause());

        prevButton.setOnAction(e -> {
            Song prev = playbackQueue.prev();
            if (prev == null) return;

            songsListView.getSelectionModel().select(playbackQueue.getCurrentIndex());
            selectedSong = prev;
            selectedLabel.setText("Selected: " + prev);

            audioPlayer.loadAndPlay(prev);
            playPauseButton.setText("Pause");
            updateNowPlayingHint();

            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());

            refreshPlaylistButtons();
        });

        nextButton.setOnAction(e -> {
            Song next = playbackQueue.next();
            if (next == null) return;

            songsListView.getSelectionModel().select(playbackQueue.getCurrentIndex());
            selectedSong = next;
            selectedLabel.setText("Selected: " + next);

            audioPlayer.loadAndPlay(next);
            playPauseButton.setText("Pause");
            updateNowPlayingHint();

            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());

            refreshPlaylistButtons();
        });

        newPlaylistButton.setOnAction(e -> handleCreatePlaylist());
        addToPlaylistButton.setOnAction(e -> handleAddSelectedToPlaylist());
        removeFromPlaylistButton.setOnAction(e -> handleRemoveSelectedFromPlaylist());

        playlistsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                refreshPlaylistButtons();
                return;
            }

            if (newVal.equals("Library")) {
                switchToLibraryView();
            } else {
                switchToPlaylistView(newVal);
            }

            refreshPlaylistButtons();
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));
    }

    private void handlePlayPause() {
        if (selectedSong == null) {
            statusLabel.setText("Select a song first.");
            return;
        }

        try {
            if (audioPlayer.getCurrentSong() == null || !selectedSong.equals(audioPlayer.getCurrentSong())) {
                audioPlayer.loadAndPlay(selectedSong);
                playPauseButton.setText("Pause");

                prevButton.setDisable(!playbackQueue.hasPrev());
                nextButton.setDisable(!playbackQueue.hasNext());

                statusLabel.setText("");
                updateNowPlayingHint();
                refreshPlaylistButtons();
                return;
            }

            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
                playPauseButton.setText("Play");
            } else {
                audioPlayer.resume();
                playPauseButton.setText("Pause");
            }

            statusLabel.setText("");
            updateNowPlayingHint();
            refreshPlaylistButtons();

        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Playback error: " + ex.getMessage());
            updateNowPlayingHint();
            refreshPlaylistButtons();
        } catch (RuntimeException ex) {
            statusLabel.setText("Playback failed (unsupported file or audio backend issue).");
            updateNowPlayingHint();
            refreshPlaylistButtons();
        }
    }

    private void handleImportFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");

        File selected = chooser.showDialog(root.getScene().getWindow());
        if (selected == null) return;

        try {
            Path folderPath = selected.toPath();
            libraryService.importFolder(folderPath);

            librarySongsSnapshot = libraryService.getAllSongs();
            displayedSongs.setAll(librarySongsSnapshot);

            playbackQueue.setQueue(displayedSongs);

            audioPlayer.stopAndDispose();
            selectedSong = null;
            songsListView.getSelectionModel().clearSelection();

            selectedLabel.setText("Selected: —");
            updateNowPlayingHint();

            playPauseButton.setDisable(true);
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            playPauseButton.setText("Play");

            showingPlaylist = false;
            activePlaylistName = null;

            statusLabel.setText(
                    librarySongsSnapshot.isEmpty()
                            ? "No supported audio files found."
                            : "Imported " + librarySongsSnapshot.size() + " song(s) from: " + selected.getName()
            );

            refreshPlaylistButtons();

        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Import failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText("Import failed due to an unexpected error.");
        }
    }

    private void updateNowPlayingHint() {
        Song playing = audioPlayer.getCurrentSong();

        boolean shouldShow =
                playing != null
                        && audioPlayer.isPlaying()
                        && selectedSong != null
                        && !selectedSong.equals(playing);

        if (shouldShow) {
            nowPlayingLabel.setText("Now Playing: " + playing);
            nowPlayingLabel.setVisible(true);
            nowPlayingLabel.setManaged(true);
        } else {
            nowPlayingLabel.setVisible(false);
            nowPlayingLabel.setManaged(false);
            nowPlayingLabel.setText("");
        }
    }

    private void refreshPlaylistButtons() {
        boolean hasSong = selectedSong != null;

        boolean hasAnyPlaylist = !playlistService.getPlaylists().isEmpty();
        addToPlaylistButton.setDisable(!(hasSong && hasAnyPlaylist));

        boolean canRemove = hasSong && showingPlaylist && activePlaylistName != null;
        removeFromPlaylistButton.setDisable(!canRemove);
    }

    private void refreshPlaylistsList() {
        playlistsListView.getItems().setAll("Library");
        playlistsListView.getItems().addAll(playlistService.getPlaylists());

        if (playlistsListView.getSelectionModel().getSelectedItem() == null) {
            playlistsListView.getSelectionModel().select("Library");
        }
    }

    private void handleCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Playlist");
        dialog.setHeaderText("Create a new playlist");
        dialog.setContentText("Playlist name:");

        dialog.showAndWait().ifPresent(name -> {
            try {
                playlistService.createPlaylist(name);
                refreshPlaylistsList();
                statusLabel.setText("Created playlist: " + name.trim());
                refreshPlaylistButtons();
            } catch (IllegalArgumentException ex) {
                statusLabel.setText("Playlist error: " + ex.getMessage());
            } catch (RuntimeException ex) {
                statusLabel.setText("Playlist error: unexpected error.");
            }
        });
    }

    private void handleAddSelectedToPlaylist() {
        if (selectedSong == null) {
            statusLabel.setText("Select a song first.");
            return;
        }

        List<String> playlists = playlistService.getPlaylists();
        if (playlists.isEmpty()) {
            statusLabel.setText("Create a playlist first.");
            return;
        }

        String highlighted = playlistsListView.getSelectionModel().getSelectedItem();
        String defaultChoice =
                (highlighted != null && !highlighted.equals("Library") && playlists.contains(highlighted))
                        ? highlighted
                        : playlists.get(0);

        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, playlists);
        dialog.setTitle("Add to Playlist");
        dialog.setHeaderText("Choose a playlist");
        dialog.setContentText("Playlist:");

        dialog.showAndWait().ifPresent(name -> {
            try {
                playlistService.addSong(name, selectedSong.getId());
                statusLabel.setText("Added to " + name + ": " + selectedSong.getTitle());

                if (showingPlaylist && name.equals(activePlaylistName)) {
                    switchToPlaylistView(activePlaylistName);
                }

                refreshPlaylistButtons();
            } catch (IllegalArgumentException ex) {
                statusLabel.setText("Playlist error: " + ex.getMessage());
            } catch (RuntimeException ex) {
                statusLabel.setText("Playlist error: unexpected error.");
            }
        });
    }

    private void handleRemoveSelectedFromPlaylist() {
        if (!showingPlaylist || activePlaylistName == null) {
            statusLabel.setText("Open a playlist first to remove songs.");
            return;
        }
        if (selectedSong == null) {
            statusLabel.setText("Select a song to remove.");
            return;
        }

        try {
            playlistService.removeSong(activePlaylistName, selectedSong.getId());
            statusLabel.setText("Removed from " + activePlaylistName + ": " + selectedSong.getTitle());

            switchToPlaylistView(activePlaylistName);
            refreshPlaylistButtons();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Playlist error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText("Playlist error: unexpected error.");
        }
    }

    private void switchToPlaylistView(String playlistName) {
        showingPlaylist = true;
        activePlaylistName = playlistName;

        List<String> ids = playlistService.getSongIds(playlistName);
        List<Song> playlistSongs = libraryService.resolveSongsByIds(ids);

        String q = searchField.getText();
        displayedSongs.setAll(filterSongs(playlistSongs, q));

        songsListView.getSelectionModel().clearSelection();
        selectedSong = null;

        playbackQueue.setQueue(displayedSongs);

        playPauseButton.setDisable(true);
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        playPauseButton.setText("Play");

        selectedLabel.setText("Selected: —");
        updateNowPlayingHint();

        refreshPlaylistButtons();
    }

    private void switchToLibraryView() {
        showingPlaylist = false;
        activePlaylistName = null;

        String q = searchField.getText();
        displayedSongs.setAll(filterSongs(librarySongsSnapshot, q));

        songsListView.getSelectionModel().clearSelection();
        selectedSong = null;

        playbackQueue.setQueue(displayedSongs);

        playPauseButton.setDisable(true);
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        playPauseButton.setText("Play");

        selectedLabel.setText("Selected: —");
        updateNowPlayingHint();

        refreshPlaylistButtons();
    }

    private void applySearchFilter(String query) {
        if (showingPlaylist && activePlaylistName != null) {
            List<String> ids = playlistService.getSongIds(activePlaylistName);
            List<Song> base = libraryService.resolveSongsByIds(ids);
            displayedSongs.setAll(filterSongs(base, query));
        } else {
            displayedSongs.setAll(filterSongs(librarySongsSnapshot, query));
        }

        playbackQueue.setQueue(displayedSongs);

        songsListView.getSelectionModel().clearSelection();
        selectedSong = null;

        playPauseButton.setDisable(true);
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        playPauseButton.setText("Play");

        selectedLabel.setText("Selected: —");
        updateNowPlayingHint();

        refreshPlaylistButtons();
    }

    private List<Song> filterSongs(List<Song> base, String query) {
        if (base == null) return List.of();
        String q = (query == null) ? "" : query.trim().toLowerCase();

        if (q.isBlank()) {
            return base;
        }

        return base.stream()
                .filter(s -> containsIgnoreCase(s.getTitle(), q)
                        || containsIgnoreCase(s.getArtist(), q)
                        || containsIgnoreCase(s.getAlbum(), q))
                .toList();
    }

    private boolean containsIgnoreCase(String haystack, String needleLower) {
        if (haystack == null) return false;
        return haystack.toLowerCase().contains(needleLower);
    }
}
