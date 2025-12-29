package com.yourname.musicplayer.ui;

import com.yourname.musicplayer.domain.Song;
import com.yourname.musicplayer.player.AudioPlayer;
import com.yourname.musicplayer.player.PlaybackQueue;
import com.yourname.musicplayer.service.LibraryService;
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

public class MainView {

    private final BorderPane root = new BorderPane();

    private final TextField searchField = new TextField();
    private final Button importFolderButton = new Button("Import Folder");

    private final ListView<Song> songsListView = new ListView<>();
    private final ListView<String> playlistsListView = new ListView<>();

    private final Button prevButton = new Button("Prev");
    private final Button playPauseButton = new Button("Play");
    private final Button nextButton = new Button("Next");

    // ✅ Two separate labels:
    private final Label selectedLabel = new Label("Selected: —");
    private final Label nowPlayingLabel = new Label("Now Playing: —"); // conditional hint label

    private final Label statusLabel = new Label("");

    private final LibraryService libraryService = new LibraryService();
    private final ObservableList<Song> songsObservable = FXCollections.observableArrayList();

    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final PlaybackQueue playbackQueue = new PlaybackQueue();

    private Song selectedSong = null;

    public MainView() {
        buildLayout();
        seedPlaceholderPlaylists();
        configureListRendering();
        wireHandlers();
    }

    public Parent getRoot() {
        return root;
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

        VBox playlistsPane = new VBox(
                6,
                sectionHeader("Playlists"),
                playlistsListView,
                new HBox(8, new Button("New Playlist"), new Button("Add Selected Song"))
        );
        playlistsPane.setPrefWidth(240);
        VBox.setVgrow(playlistsListView, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(songsPane, playlistsPane);
        splitPane.setDividerPositions(0.7);
        root.setCenter(splitPane);

        prevButton.setTooltip(new Tooltip("Previous"));
        playPauseButton.setTooltip(new Tooltip("Play / Pause"));
        nextButton.setTooltip(new Tooltip("Next"));

        HBox controls = new HBox(10, prevButton, playPauseButton, nextButton);
        controls.setPadding(new Insets(10, 0, 0, 0));

        // ✅ Hide by default, and take up NO SPACE when hidden.
        nowPlayingLabel.setVisible(false);
        nowPlayingLabel.setManaged(false);

        // ✅ bottom-right: Selected + (conditional) Now Playing stacked
        VBox rightLabels = new VBox(2, selectedLabel, nowPlayingLabel);

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(12, controls, bottomSpacer, rightLabels);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(bottomBar);

        prevButton.setDisable(true);
        playPauseButton.setDisable(true);
        nextButton.setDisable(true);

        songsListView.setItems(songsObservable);

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

    private void seedPlaceholderPlaylists() {
        playlistsListView.getItems().setAll("Favorites", "Study Mix", "Gym");
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
                return;
            }

            int idx = newIdx.intValue();
            selectedSong = songsObservable.get(idx);
            playbackQueue.playAt(idx);

            selectedLabel.setText("Selected: " + selectedSong);
            updateNowPlayingHint();

            playPauseButton.setDisable(false);
            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());

            // If the selected song is currently playing, show Pause; otherwise Play.
            if (audioPlayer.getCurrentSong() != null
                    && selectedSong.equals(audioPlayer.getCurrentSong())
                    && audioPlayer.isPlaying()) {
                playPauseButton.setText("Pause");
            } else {
                playPauseButton.setText("Play");
            }
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
            updateNowPlayingHint(); // will hide (selected == playing)

            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());
        });

        nextButton.setOnAction(e -> {
            Song next = playbackQueue.next();
            if (next == null) return;

            songsListView.getSelectionModel().select(playbackQueue.getCurrentIndex());
            selectedSong = next;
            selectedLabel.setText("Selected: " + next);

            audioPlayer.loadAndPlay(next);
            playPauseButton.setText("Pause");
            updateNowPlayingHint(); // will hide (selected == playing)

            prevButton.setDisable(!playbackQueue.hasPrev());
            nextButton.setDisable(!playbackQueue.hasNext());
        });
    }

    private void handlePlayPause() {
        if (selectedSong == null) {
            statusLabel.setText("Select a song first.");
            return;
        }

        try {
            // If different song selected, load that song.
            if (audioPlayer.getCurrentSong() == null || !selectedSong.equals(audioPlayer.getCurrentSong())) {
                audioPlayer.loadAndPlay(selectedSong);
                playPauseButton.setText("Pause");

                prevButton.setDisable(!playbackQueue.hasPrev());
                nextButton.setDisable(!playbackQueue.hasNext());

                statusLabel.setText("");
                updateNowPlayingHint(); // hide after switching playback
                return;
            }

            // Same song -> toggle pause/resume.
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
                playPauseButton.setText("Play");
            } else {
                audioPlayer.resume();
                playPauseButton.setText("Pause");
            }

            statusLabel.setText("");
            updateNowPlayingHint();

        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Playback error: " + ex.getMessage());
            updateNowPlayingHint();
        } catch (RuntimeException ex) {
            statusLabel.setText("Playback failed (unsupported file or audio backend issue).");
            updateNowPlayingHint();
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

            List<Song> songs = libraryService.getAllSongs();
            songsObservable.setAll(songs);

            playbackQueue.setQueue(songsObservable);

            // Reset playback state
            audioPlayer.stopAndDispose();
            selectedSong = null;
            songsListView.getSelectionModel().clearSelection();

            selectedLabel.setText("Selected: —");
            updateNowPlayingHint();

            playPauseButton.setDisable(true);
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            playPauseButton.setText("Play");

            statusLabel.setText(
                    songs.isEmpty()
                            ? "No supported audio files found."
                            : "Imported " + songs.size() + " song(s) from: " + selected.getName()
            );

        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Import failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText("Import failed due to an unexpected error.");
        }
    }

    // ✅ Only show when something is actively playing AND user has selected a different song.
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
}
