package com.yourname.musicplayer.ui;

import com.yourname.musicplayer.domain.Song;
import com.yourname.musicplayer.player.AudioPlayer;
import com.yourname.musicplayer.service.LibraryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
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

    private final Button prevButton = new Button("⏹");
    private final Button playPauseButton = new Button("▶");
    private final Button nextButton = new Button("⏭");
    private final Label nowPlayingLabel = new Label("Now Playing: —");

    private final Label statusLabel = new Label("");

    private final LibraryService libraryService = new LibraryService();
    private final ObservableList<Song> songsObservable = FXCollections.observableArrayList();

    private final AudioPlayer audioPlayer = new AudioPlayer();
    private Song selectedSong = null;

    public MainView() {
        buildLayout();
        seedPlaceholderPlaylists();
        configureListRendering();
        wireHandlers();

        // Surface MediaPlayer errors into the UI (and keep them visible)
        audioPlayer.setErrorListener(msg -> Platform.runLater(() -> {
            statusLabel.setText("Playback failed: " + msg);
            playPauseButton.setText("▶");
        }));
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

        Button newPlaylistButton = new Button("New Playlist");
        Button addToPlaylistButton = new Button("Add Selected Song");

        HBox playlistActions = new HBox(8, newPlaylistButton, addToPlaylistButton);
        VBox playlistsPane = new VBox(
                6,
                sectionHeader("Playlists"),
                playlistsListView,
                playlistActions
        );
        playlistsPane.setPrefWidth(240);
        VBox.setVgrow(playlistsListView, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(songsPane, playlistsPane);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.70);
        root.setCenter(splitPane);

        HBox controls = new HBox(10, prevButton, playPauseButton, nextButton);
        controls.setPadding(new Insets(10, 0, 0, 0));

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(12, controls, bottomSpacer, nowPlayingLabel);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));
        root.setBottom(bottomBar);

        prevButton.setDisable(true);
        playPauseButton.setDisable(true);
        nextButton.setDisable(true);

        newPlaylistButton.setDisable(true);
        addToPlaylistButton.setDisable(true);

        songsListView.setItems(songsObservable);

        statusLabel.setWrapText(true);
        statusLabel.setMinHeight(18);
        statusLabel.setText("");
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
                setText((empty || song == null) ? null : song.toString());
            }
        });
    }

    private void seedPlaceholderPlaylists() {
        playlistsListView.getItems().setAll(
                "Favorites",
                "Study Mix",
                "Gym"
        );
    }

    private void wireHandlers() {
        songsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedSong = newVal;

            if (newVal != null) {
                nowPlayingLabel.setText("Selected: " + newVal);
                playPauseButton.setDisable(false);
                prevButton.setDisable(false);
                nextButton.setDisable(true);
            } else {
                nowPlayingLabel.setText("Now Playing: —");
                playPauseButton.setDisable(true);
                prevButton.setDisable(true);
                nextButton.setDisable(true);
            }

            playPauseButton.setText("▶");

            if (newVal != null) {
                System.out.println("[UI] Selected song path: " + newVal.getPath());
            }
        });

        importFolderButton.setOnAction(e -> handleImportFolder());
        playPauseButton.setOnAction(e -> handlePlayPause());

        prevButton.setOnAction(e -> {
            audioPlayer.stopAndDispose();
            playPauseButton.setText("▶");
            nowPlayingLabel.setText("Stopped.");
            statusLabel.setText("");
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // later merge
        });
    }

    private void handlePlayPause() {
        if (selectedSong == null) {
            statusLabel.setText("Select a song first.");
            return;
        }

        try {
            if (audioPlayer.getCurrentSong() == null || !selectedSong.equals(audioPlayer.getCurrentSong())) {
                statusLabel.setText(""); // clear old errors before starting
                audioPlayer.loadAndPlay(selectedSong);
                nowPlayingLabel.setText("Now Playing: " + selectedSong);
                playPauseButton.setText("⏸");
                return;
            }

            if (audioPlayer.isPlaying()) {
                audioPlayer.pause();
                playPauseButton.setText("▶");
                nowPlayingLabel.setText("Paused: " + selectedSong);
            } else {
                audioPlayer.resume();
                playPauseButton.setText("⏸");
                nowPlayingLabel.setText("Now Playing: " + selectedSong);
            }

        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Playback error: " + ex.getMessage());
        } catch (RuntimeException ex) {
            // Synchronous errors only; async errors go through AudioPlayer's errorListener
            ex.printStackTrace();
            statusLabel.setText("Playback failed (see console for details).");
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

            selectedSong = null;
            songsListView.getSelectionModel().clearSelection();
            audioPlayer.stopAndDispose();
            playPauseButton.setText("▶");
            nowPlayingLabel.setText("Now Playing: —");

            prevButton.setDisable(true);
            playPauseButton.setDisable(true);
            nextButton.setDisable(true);

            if (songs.isEmpty()) {
                statusLabel.setText("No supported audio files found in that folder (.mp3, .wav, .m4a).");
            } else {
                statusLabel.setText("Imported " + songs.size() + " song(s) from: " + selected.getName());
            }

        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Import failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            statusLabel.setText("Import failed due to an unexpected error (see console).");
        }
    }
}
