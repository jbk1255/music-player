package com.yourname.musicplayer;

import com.yourname.musicplayer.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView();

        Scene scene = new Scene(mainView.getRoot(), 1000, 620);

        scene.getStylesheets().add(
                getClass().getResource("/com/yourname/musicplayer/ui/UIStyles.css").toExternalForm()
        );

        stage.setTitle("Music Player");
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
