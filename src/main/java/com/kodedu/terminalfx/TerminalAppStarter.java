package com.kodedu.terminalfx;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.kodedu.terminalfx.helper.ThreadHelper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class TerminalAppStarter extends Application {

	public static Stage mainStage;
	@Override
	public void start(Stage stage) throws Exception {
		mainStage = stage;
		ExplorerController explorerController = null;
		FXMLController fxmlController = null;
		Adapter adapter = new Adapter();
		Node[] childrens = new Node[2];
		try (InputStream sceneStream = TerminalAppStarter.class.getResourceAsStream("/fxml/Explorer.fxml")) {
			FXMLLoader loader = new FXMLLoader();
			Parent root2 = loader.load(sceneStream);
			explorerController = loader.getController();
			childrens[0] = root2;
			adapter.setExplorerController(explorerController);
		}

		try (InputStream sceneStream = TerminalAppStarter.class.getResourceAsStream("/fxml/Terminal_Scene.fxml")) {
			FXMLLoader loader = new FXMLLoader();
			Parent root1 = loader.load(sceneStream);
			fxmlController = loader.getController();
			childrens[1] = root1;
			adapter.setFxmlController(fxmlController);
		}

		var pane = new SplitPane(childrens);
		pane.setDividerPosition(0, 0.15);
		pane.setDividerPosition(1, 0.8);
		
		explorerController.setAdapter(adapter);
		fxmlController.setAdapter(adapter);

		Scene scene = new Scene(pane);
		scene.getStylesheets().add(TerminalAppStarter.class.getResource("/styles/Styles.css").toExternalForm());

		stage.setTitle("TerminalFX");
		stage.setScene(scene);
		stage.show();
		stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::onCloseRequested);
		
		final ExplorerController finalExplorerController = explorerController;
		TerminalAppStarter.addOnCloseRequest(ev -> {
			if (finalExplorerController.isDirty()) {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "저장되지않는 파일이 있습니다. 종료할까요?", ButtonType.YES, ButtonType.NO);
				alert.setTitle("Confirm Close");
				alert.setHeaderText(null);
				alert.showAndWait().ifPresent(response -> {
					if (response != ButtonType.YES) {
						ev.consume();
					}
				});
			}
		});
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
	
	private static List<Consumer<WindowEvent>> registeredCloseEvents = new ArrayList<>();
	@SuppressWarnings("exports")
	public static void addOnCloseRequest(Consumer<WindowEvent> handle) {
		registeredCloseEvents.add(handle);	
	}

	/**
	 * @param <T>
	 * @param t1
	 */
	private <T extends Event> void onCloseRequested(WindowEvent t1) {
		registeredCloseEvents.forEach(a ->{
			try {
				a.accept(t1);
			} catch(Exception ex) {ex.printStackTrace(); }
		});
	}
	
}
