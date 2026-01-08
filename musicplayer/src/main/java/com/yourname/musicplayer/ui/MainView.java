package com.yourname.musicplayer.ui;

import com.yourname.musicplayer.domain.Song;
import com.yourname.musicplayer.persistence.JsonStore;
import com.yourname.musicplayer.player.AudioPlayer;
import com.yourname.musicplayer.player.PlaybackQueue;
import com.yourname.musicplayer.service.LibraryService;
import com.yourname.musicplayer.service.PlaylistService;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainView {

    private final BorderPane root = new BorderPane();

    private final TextField searchField = new TextField();
    private final Button importFolderButton = new Button("Import Folder");
    private final Button resetLibraryButton = new Button("Reset Library");

    private final ListView<Song> songsListView = new ListView<>();
    private final ListView<String> playlistsListView = new ListView<>();

    private final Button prevButton = new Button();
    private final Button playPauseButton = new Button();
    private final Button nextButton = new Button();

    private final Label activeViewLabel = new Label("Viewing: Library");

    private final Label nowPlayingLabel = new Label("Not Playing");
    private final Label selectedLabel = new Label("Selected: —");
    private final Label statusLabel = new Label("");

    private final LibraryService libraryService = new LibraryService();
    private final PlaylistService playlistService = new PlaylistService();

    private final javafx.collections.ObservableList<Song> displayedSongs =
            javafx.collections.FXCollections.observableArrayList();
    private List<Song> librarySongsSnapshot = List.of();

    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final PlaybackQueue playbackQueue = new PlaybackQueue();

    private Song selectedSong = null;

    private boolean showingPlaylist = false;
    private String activePlaylistName = null;

    private final Button newPlaylistButton = new Button("New Playlist");
    private final Button addToPlaylistButton = new Button("Add to Playlist");
    private final Button removeFromPlaylistButton = new Button("Remove Song");
    private final Button deletePlaylistButton = new Button("Delete Playlist");

    private final JsonStore jsonStore = new JsonStore();

    private final Slider progressSlider = new Slider(0, 1, 0);
    private final Label currentTimeLabel = new Label("0:00");
    private final Label durationLabel = new Label("0:00");
    private boolean userDragging = false;

    private AnimationTimer progressTimer;

    private final SVGPath iconPrev = makeIcon("M6 6h2v12H6z M9.5 12l10 6V6z");
    private final SVGPath iconNext = makeIcon("M6 6l10 6-10 6V6z M18 6h2v12h-2z");
    private final SVGPath iconPlay = makeIcon("M8 6v12l10-6z");
    private final SVGPath iconPause = makeIcon("M7 6h4v12H7z M13 6h4v12h-4z");

    private static final String LOGO_PATH = "/com/yourname/musicplayer/ui/playra_logo_p.png";

    public MainView() {
        buildLayout();
        configureListRendering();
        wireHandlers();

        loadState();

        refreshPlaylistsList();
        refreshPlaylistButtons();

        initProgressBar();
        setActiveViewLabel();
        updateNowPlayingHint();
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

        importFolderButton.getStyleClass().add("primary-btn");

        ImageView logoView = buildLogoView();

        Label title = new Label("Playra");
        title.getStyleClass().add("title");

        HBox brand = new HBox(12);
        brand.getStyleClass().add("brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        if (logoView != null) {
            brand.getChildren().add(logoView);
        }
        brand.getChildren().add(title);

        searchField.setPromptText("Search songs, artists, albums...");
        searchField.setMinWidth(260);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, brand, spacer, searchField, importFolderButton, resetLibraryButton);
        topBar.setPadding(new Insets(8, 0, 12, 0));
        topBar.setMinHeight(72);
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

        activeViewLabel.getStyleClass().add("active-view");

        VBox songsPane = new VBox(6, activeViewLabel, statusLabel, songsListView);
        songsPane.setPadding(new Insets(0, 10, 0, 0));
        VBox.setVgrow(songsListView, Priority.ALWAYS);

        HBox playlistActions = new HBox(8, newPlaylistButton, addToPlaylistButton, removeFromPlaylistButton, deletePlaylistButton);
        playlistActions.setAlignment(Pos.CENTER_LEFT);

        addToPlaylistButton.setDisable(true);
        removeFromPlaylistButton.setDisable(true);
        deletePlaylistButton.setDisable(true);

        VBox playlistsPane = new VBox(
                6,
                sectionHeader("Playlists"),
                playlistsListView,
                playlistActions
        );

        playlistsPane.getStyleClass().add("sidebar");

        playlistsPane.setPrefWidth(360);
        VBox.setVgrow(playlistsListView, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(songsPane, playlistsPane);
        splitPane.setDividerPositions(0.7);
        root.setCenter(splitPane);

        setupIconButton(prevButton, iconPrev, "Previous");
        setupIconButton(playPauseButton, iconPlay, "Play / Pause");
        setupIconButton(nextButton, iconNext, "Next");

        HBox controls = new HBox(10, prevButton, playPauseButton, nextButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        nowPlayingLabel.getStyleClass().add("now-playing");
        selectedLabel.getStyleClass().add("selected-weak");

        VBox rightLabels = new VBox(2, nowPlayingLabel, selectedLabel);
        rightLabels.setAlignment(Pos.CENTER_RIGHT);

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        progressSlider.setDisable(true);
        progressSlider.setMin(0);
        progressSlider.setMax(1);
        progressSlider.setValue(0);

        HBox progressRow = new HBox(10, currentTimeLabel, progressSlider, durationLabel);
        progressRow.getStyleClass().add("player-progress-row");
        progressRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        HBox bottomButtonsRow = new HBox(12, controls, bottomSpacer, rightLabels);
        bottomButtonsRow.setAlignment(Pos.CENTER_LEFT);

        VBox bottomBar = new VBox(6, progressRow, bottomButtonsRow);
        bottomBar.getStyleClass().add("player-bar");
        bottomBar.setPadding(new Insets(10, 10, 10, 10));
        root.setBottom(bottomBar);

        prevButton.setDisable(true);
        playPauseButton.setDisable(true);
        nextButton.setDisable(true);

        songsListView.setItems(displayedSongs);

        statusLabel.setWrapText(true);
        statusLabel.setMinHeight(18);
    }

    private ImageView buildLogoView() {
        try (InputStream is = getClass().getResourceAsStream(LOGO_PATH)) {
            if (is == null) return null;

            ImageView iv = new ImageView(new Image(is));
            iv.getStyleClass().add("brand-logo");
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            iv.setFitHeight(44);

            return iv;
        } catch (Exception ignored) {
            return null;
        }
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

                playPauseButton.setDisable(true);
                prevButton.setDisable(true);
                nextButton.setDisable(true);
                setPlayPauseIcon(false);

                resetProgressUI();
                updateNowPlayingHint();
                refreshPlaylistButtons();
                return;
            }

            int idx = newIdx.intValue();
            selectedSong = displayedSongs.get(idx);
            playbackQueue.playAt(idx);

            selectedLabel.setText("Selected: " + selectedSong);

            playPauseButton.setDisable(false);
            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());

            boolean isSameAndPlaying =
                    audioPlayer.getCurrentSong() != null
                            && selectedSong.equals(audioPlayer.getCurrentSong())
                            && audioPlayer.isPlaying();

            setPlayPauseIcon(isSameAndPlaying);

            updateNowPlayingHint();
            refreshPlaylistButtons();
        });

        importFolderButton.setOnAction(e -> handleImportFolder());
        playPauseButton.setOnAction(e -> handlePlayPause());
        resetLibraryButton.setOnAction(e -> handleResetLibrary());

        prevButton.setOnAction(e -> {
            Song prev = playbackQueue.prev();
            if (prev == null) return;

            songsListView.getSelectionModel().select(playbackQueue.getCurrentIndex());
            selectedSong = prev;
            selectedLabel.setText("Selected: " + prev);

            audioPlayer.loadAndPlay(prev);
            afterTrackLoadedSetup();

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
            afterTrackLoadedSetup();

            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());

            refreshPlaylistButtons();
        });

        newPlaylistButton.setOnAction(e -> handleCreatePlaylist());
        addToPlaylistButton.setOnAction(e -> handleAddSelectedToPlaylist());
        removeFromPlaylistButton.setOnAction(e -> handleRemoveSelectedFromPlaylist());
        deletePlaylistButton.setOnAction(e -> handleDeletePlaylist());

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

            setActiveViewLabel();
            refreshPlaylistButtons();
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));
    }

    private void initProgressBar() {
        progressSlider.setOnMousePressed(e -> userDragging = true);
        progressSlider.setOnMouseReleased(e -> {
            if (!audioPlayer.hasMedia()) {
                userDragging = false;
                return;
            }

            Duration total = audioPlayer.getTotalDuration();
            if (total == null || total.isUnknown() || total.lessThanOrEqualTo(Duration.ZERO)) {
                userDragging = false;
                return;
            }

            double seconds = progressSlider.getValue();
            audioPlayer.seek(Duration.seconds(seconds));
            userDragging = false;
        });

        progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> userDragging = isChanging);

        progressTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!audioPlayer.hasMedia()) return;

                Duration total = audioPlayer.getTotalDuration();
                Duration current = audioPlayer.getCurrentTime();

                if (total == null || total.isUnknown() || total.lessThanOrEqualTo(Duration.ZERO)) return;
                if (current == null) current = Duration.ZERO;

                durationLabel.setText(formatTime(total));

                if (!userDragging) {
                    progressSlider.setDisable(false);
                    progressSlider.setMax(total.toSeconds());
                    progressSlider.setValue(current.toSeconds());
                }

                currentTimeLabel.setText(formatTime(current));
            }
        };
        progressTimer.start();
    }

    private void afterTrackLoadedSetup() {
        resetProgressUI();

        audioPlayer.setOnReady(() -> Platform.runLater(() -> {
            Duration total = audioPlayer.getTotalDuration();
            if (total == null || total.isUnknown() || total.lessThanOrEqualTo(Duration.ZERO)) {
                resetProgressUI();
                updateNowPlayingHint();
                return;
            }
            progressSlider.setDisable(false);
            progressSlider.setMax(total.toSeconds());
            durationLabel.setText(formatTime(total));
            updateNowPlayingHint();
        }));

        audioPlayer.setOnPlaying(() -> Platform.runLater(() -> {
            setPlayPauseIcon(true);
            updateNowPlayingHint();
        }));

        audioPlayer.setOnPaused(() -> Platform.runLater(() -> {
            setPlayPauseIcon(false);
            updateNowPlayingHint();
        }));

        audioPlayer.setOnStopped(() -> Platform.runLater(() -> {
            setPlayPauseIcon(false);
            updateNowPlayingHint();
        }));

        audioPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
            Song next = playbackQueue.next();
            if (next == null) {
                setPlayPauseIcon(false);
                updateNowPlayingHint();
                return;
            }

            songsListView.getSelectionModel().select(playbackQueue.getCurrentIndex());
            selectedSong = next;
            selectedLabel.setText("Selected: " + next);

            audioPlayer.loadAndPlay(next);
            afterTrackLoadedSetup();

            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());

            refreshPlaylistButtons();
        }));
    }

    private void resetProgressUI() {
        progressSlider.setDisable(true);
        progressSlider.setMin(0);
        progressSlider.setMax(1);
        progressSlider.setValue(0);
        currentTimeLabel.setText("0:00");
        durationLabel.setText("0:00");
        userDragging = false;
    }

    private String formatTime(Duration d) {
        if (d == null || d.isUnknown()) return "0:00";

        int totalSeconds = (int) Math.floor(d.toSeconds());
        if (totalSeconds < 0) totalSeconds = 0;

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return minutes + ":" + String.format("%02d", seconds);
    }

    private void setActiveViewLabel() {
        if (showingPlaylist && activePlaylistName != null && !activePlaylistName.isBlank()) {
            activeViewLabel.setText("Viewing: " + activePlaylistName);
        } else {
            activeViewLabel.setText("Viewing: Library");
        }
    }

    private void updateNowPlayingHint() {
        Song playing = audioPlayer.getCurrentSong();

        if (playing != null && audioPlayer.isPlaying()) {
            nowPlayingLabel.setText("Now Playing: " + playing);
        } else if (playing != null) {
            nowPlayingLabel.setText("Paused: " + playing);
        } else {
            nowPlayingLabel.setText("Not Playing");
        }
    }

    private void setupIconButton(Button btn, SVGPath icon, String tooltip) {
        btn.getStyleClass().addAll("icon-btn");
        btn.setGraphic(icon);
        btn.setText(null);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setFocusTraversable(false);
        btn.setMinWidth(44);
        btn.setMinHeight(34);
    }

    private void setPlayPauseIcon(boolean playing) {
        playPauseButton.setGraphic(playing ? iconPause : iconPlay);
    }

    private SVGPath makeIcon(String path) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("icon");
        return svg;
    }

    private void handleResetLibrary() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Library");
        confirm.setHeaderText("Clear all songs from the library?");
        confirm.setContentText("Playlists will remain, but their songs won’t show/play until you import again.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        audioPlayer.stopAndDispose();
        resetProgressUI();

        libraryService.clearLibrary();
        librarySongsSnapshot = List.of();

        showingPlaylist = false;
        activePlaylistName = null;
        setActiveViewLabel();

        displayedSongs.clear();
        playbackQueue.setQueue(displayedSongs);

        songsListView.getSelectionModel().clearSelection();
        selectedSong = null;

        selectedLabel.setText("Selected: —");

        playPauseButton.setDisable(true);
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        setPlayPauseIcon(false);

        statusLabel.setText("Library cleared. Import a folder to add songs again.");
        refreshPlaylistButtons();

        saveState();
        playlistsListView.getSelectionModel().select("Library");

        updateNowPlayingHint();
    }

    private void handleDeletePlaylist() {
        String selected = playlistsListView.getSelectionModel().getSelectedItem();

        if (selected == null || selected.equals("Library")) {
            statusLabel.setText("Cannot delete Library.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Playlist");
        confirm.setHeaderText("Delete playlist \"" + selected + "\"?");
        confirm.setContentText("This cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            playlistService.deletePlaylist(selected);

            showingPlaylist = false;
            activePlaylistName = null;
            setActiveViewLabel();

            refreshPlaylistsList();
            playlistsListView.getSelectionModel().select("Library");
            switchToLibraryView();

            refreshPlaylistButtons();
            saveState();

            statusLabel.setText("Deleted playlist: " + selected);
        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Playlist error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText("Playlist error: unexpected error.");
        }
    }

    private void handlePlayPause() {
        if (selectedSong == null) {
            statusLabel.setText("Select a song first.");
            return;
        }

        try {
            if (audioPlayer.getCurrentSong() == null || !selectedSong.equals(audioPlayer.getCurrentSong())) {
                audioPlayer.loadAndPlay(selectedSong);
                afterTrackLoadedSetup();

                prevButton.setDisable(!playbackQueue.hasPrev());
                nextButton.setDisable(!playbackQueue.hasNext());

                statusLabel.setText("");
                refreshPlaylistButtons();
                return;
            }

            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
            } else {
                audioPlayer.resume();
            }

            statusLabel.setText("");
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
            int before = libraryService.getAllSongs().size();

            Path folderPath = selected.toPath();
            libraryService.importFolder(folderPath);

            librarySongsSnapshot = libraryService.getAllSongs();
            int after = librarySongsSnapshot.size();
            int added = after - before;

            if (showingPlaylist && activePlaylistName != null) {
                switchToPlaylistView(activePlaylistName);
            } else {
                displayedSongs.setAll(filterSongs(librarySongsSnapshot, searchField.getText()));
                playbackQueue.setQueue(displayedSongs);

                audioPlayer.stopAndDispose();
                resetProgressUI();

                selectedSong = null;
                songsListView.getSelectionModel().clearSelection();

                selectedLabel.setText("Selected: —");
                updateNowPlayingHint();

                playPauseButton.setDisable(true);
                prevButton.setDisable(true);
                nextButton.setDisable(true);
                setPlayPauseIcon(false);
            }

            statusLabel.setText(
                    added <= 0
                            ? "No new supported audio files found in: " + selected.getName()
                            : "Added " + added + " song(s) from: " + selected.getName() + " (Total: " + after + ")"
            );

            refreshPlaylistButtons();
            saveState();

        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Import failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText("Import failed due to an unexpected error.");
        }
    }

    private void refreshPlaylistButtons() {
        boolean hasSong = selectedSong != null;

        boolean hasAnyPlaylist = !playlistService.getPlaylists().isEmpty();
        addToPlaylistButton.setDisable(!(hasSong && hasAnyPlaylist));

        boolean canRemoveSong = hasSong && showingPlaylist && activePlaylistName != null;
        removeFromPlaylistButton.setDisable(!canRemoveSong);

        String selectedPlaylist = playlistsListView.getSelectionModel().getSelectedItem();
        boolean canDeletePlaylist = selectedPlaylist != null && !selectedPlaylist.equals("Library");
        deletePlaylistButton.setDisable(!canDeletePlaylist);
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
                saveState();
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
                saveState();
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
            saveState();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Playlist error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText("Playlist error: unexpected error.");
        }
    }

    private void switchToPlaylistView(String playlistName) {
        showingPlaylist = true;
        activePlaylistName = playlistName;
        setActiveViewLabel();

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
        setPlayPauseIcon(false);

        selectedLabel.setText("Selected: —");
        resetProgressUI();
        refreshPlaylistButtons();

        if (!ids.isEmpty() && playlistSongs.isEmpty()) {
            statusLabel.setText("This playlist has songs, but they are not currently in the library. Import the folder again.");
        }
    }

    private void switchToLibraryView() {
        showingPlaylist = false;
        activePlaylistName = null;
        setActiveViewLabel();

        String q = searchField.getText();
        displayedSongs.setAll(filterSongs(librarySongsSnapshot, q));

        songsListView.getSelectionModel().clearSelection();
        selectedSong = null;

        playbackQueue.setQueue(displayedSongs);

        playPauseButton.setDisable(true);
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        setPlayPauseIcon(false);

        selectedLabel.setText("Selected: —");
        resetProgressUI();
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
        setPlayPauseIcon(false);

        selectedLabel.setText("Selected: —");
        resetProgressUI();
        refreshPlaylistButtons();
    }

    private List<Song> filterSongs(List<Song> base, String query) {
        if (base == null) return List.of();
        String q = (query == null) ? "" : query.trim().toLowerCase();

        if (q.isBlank()) return base;

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
