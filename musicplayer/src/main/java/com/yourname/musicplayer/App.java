
package com.yourname.musicplayer;

import com.yourname.musicplayer.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class App extends Application {

    private static final String LOGO_PATH = "/com/yourname/musicplayer/ui/playra_logo_p.png";

    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView();

        Scene scene = new Scene(mainView.getRoot(), 1800, 800);

        scene.getStylesheets().add(
                getClass().getResource("/com/yourname/musicplayer/ui/UIStyles.css").toExternalForm()
        );

        stage.setTitle("Playra");

        try (InputStream is = getClass().getResourceAsStream(LOGO_PATH)) {
            if (is != null) {
                stage.getIcons().add(new Image(is));
            }
        } catch (Exception ignored) { }

        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            try {
                mainView.saveState();
            } catch (RuntimeException ex) {
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
