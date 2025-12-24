package com.yourname.musicplayer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
public class MainView {

    private final BorderPane root = new BorderPane();

    private final TextField searchField = new TextField();
    private final Button importFolderButton = new Button("Import Folder");

    private final ListView<String> songsListView = new ListView<>();
    private final ListView<String> playlistsListView = new ListView<>();

    private final Button prevButton = new Button("⏮");
    private final Button playPauseButton = new Button("▶");
    private final Button nextButton = new Button("⏭");
    private final Label nowPlayingLabel = new Label("Now Playing: —");

    public MainView() {
        buildLayout();
        seedPlaceholderData();
        wirePlaceholderHandlers();
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

        VBox songsPane = new VBox(6, sectionHeader("Library"), songsListView);
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
    }

    private Label sectionHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-header");
        return label;
    }

    private void seedPlaceholderData() {
        songsListView.getItems().addAll(
                "Example Song 1 — Unknown Artist",
                "Example Song 2 — Unknown Artist",
                "Example Song 3 — Unknown Artist"
        );

        playlistsListView.getItems().addAll(
                "Favorites",
                "Study Mix",
                "Gym"
        );
    }

    private void wirePlaceholderHandlers() {
        songsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                nowPlayingLabel.setText("Selected: " + newVal);
            } else {
                nowPlayingLabel.setText("Now Playing: —");
            }
        });

        importFolderButton.setOnAction(e ->
                new Alert(Alert.AlertType.INFORMATION, "Import will be implemented in a later merge.").showAndWait()
        );

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
        });
    }
}
