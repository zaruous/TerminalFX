package com.kodedu.terminalfx;

import java.io.InputStream;

import com.kodedu.terminalfx.helper.ThreadHelper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

public class TerminalAppStarter extends Application {

	@Override
	public void start(Stage stage) throws Exception {

		ExplorerController explorerController = null;
		FXMLController fxmlController = null;
		Adapter adapter = new Adapter();
		var pane = new SplitPane();
		pane.setDividerPosition(0, 0.3);
		try (InputStream sceneStream = TerminalAppStarter.class.getResourceAsStream("/fxml/Explorer.fxml")) {
			FXMLLoader loader = new FXMLLoader();
			Parent root2 = loader.load(sceneStream);
			explorerController = loader.getController();
			pane.getItems().add(root2);
			adapter.setExplorerController(explorerController);
		}

		try (InputStream sceneStream = TerminalAppStarter.class.getResourceAsStream("/fxml/Terminal_Scene.fxml")) {
			FXMLLoader loader = new FXMLLoader();
			Parent root1 = loader.load(sceneStream);
			fxmlController = loader.getController();
			pane.getItems().add(root1);
			adapter.setFxmlController(fxmlController);
		}

		explorerController.setAdapter(adapter);
		fxmlController.setAdapter(adapter);

		Scene scene = new Scene(pane);
		scene.getStylesheets().add(TerminalAppStarter.class.getResource("/styles/Styles.css").toExternalForm());

		stage.setTitle("TerminalFX");
		stage.setScene(scene);
		stage.show();
	}

	@Override
	public void stop() throws Exception {
		ThreadHelper.stopExecutorService();
		Platform.exit();
		System.exit(0);
	}

	/**
	 * The main() method is ignored in correctly deployed JavaFX application. main()
	 * serves only as fallback in case the application can not be launched through
	 * deployment artifacts, e.g., in IDEs with limited FX support. NetBeans ignores
	 * main().
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
