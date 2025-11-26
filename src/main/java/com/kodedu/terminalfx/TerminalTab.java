package com.kodedu.terminalfx;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;

import com.kodedu.terminalfx.config.TabNameGenerator;
import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.helper.IOHelper;
import com.kodedu.terminalfx.helper.ThreadHelper;
import com.pty4j.PtyProcess;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

/**
 * 
 */
public class TerminalTab extends Tab {

    private final Terminal terminal;
    private final TabNameGenerator tabNameGenerator;
    private static final String NEW_TAB_KEY = "T";

    public TerminalTab(TerminalConfig terminalConfig, TabNameGenerator tabNameGenerator, Path terminalPath) {
        this(new Terminal(terminalConfig, terminalPath), tabNameGenerator);
    }

    public TerminalTab(Terminal terminal, TabNameGenerator tabNameGenerator) {
        this.terminal = terminal;
        this.tabNameGenerator = tabNameGenerator;

        this.terminal.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            if (event.isShortcutDown() && NEW_TAB_KEY.equalsIgnoreCase(event.getText())) {
                newTerminal();
            }
        });

        this.setOnCloseRequest(event -> {
            event.consume();
            closeTerminal();
        });

        final String tabName = getTabNameGenerator().next();
        setText(tabName);

        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem newTab = new MenuItem("New Tab");
        final MenuItem closeTab = new MenuItem("Close");
        final MenuItem closeOthers = new MenuItem("Close Others");
        final MenuItem closeAll = new MenuItem("Close All");
        
        // Rename Tab 메뉴 아이템 추가
        final MenuItem renameTab = new MenuItem("Rename Tab");

        newTab.setOnAction(this::newTerminal);
        closeTab.setOnAction(this::closeTerminal);
        closeAll.setOnAction(this::closeAllTerminal);
        closeOthers.setOnAction(this::closeOtherTerminals);
        renameTab.setOnAction(event -> {
			//rename 다이얼로그 표시
			Dialog<String> dialog = new Dialog<>();
			dialog.setTitle("Rename Tab");
			dialog.setHeaderText("Enter new tab name:");
			HBox hBox = new HBox();
			javafx.scene.control.TextField textField = new javafx.scene.control.TextField();
			hBox.getChildren().add(textField);
			dialog.getDialogPane().setContent(hBox);
			dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
			dialog.setResultConverter(dialogButton -> {
			    if (dialogButton == javafx.scene.control.ButtonType.OK) {
			        return textField.getText();
			    }
			    return null;
			});
			java.util.Optional<String> result = dialog.showAndWait();
			result.ifPresent(newName -> {
			    if (!newName.trim().isEmpty()) {
			        setText(newName.trim());
			    }
			});
			
		});
        contextMenu.getItems().addAll(newTab, renameTab, closeTab, closeOthers, closeAll);
        this.setContextMenu(contextMenu);

        setContent(terminal);
    }

    private void closeOtherTerminals(ActionEvent actionEvent) {
        final ObservableList<Tab> tabs = FXCollections.observableArrayList(this.getTabPane().getTabs());
        for (final Tab tab : tabs) {
            if (tab instanceof TerminalTab) {
                if (tab != this) {
                    ((TerminalTab) tab).closeTerminal();
                }
            }
        }
    }

    private void closeAllTerminal(ActionEvent actionEvent) {
        final ObservableList<Tab> tabs = FXCollections.observableArrayList(this.getTabPane().getTabs());
        for (final Tab tab : tabs) {
            if (tab instanceof TerminalTab) {
                ((TerminalTab) tab).closeTerminal();
            }
        }
    }

    public void newTerminal(ActionEvent... actionEvent) {
        final TerminalTab terminalTab = new TerminalTab(getTerminalConfig(), getTabNameGenerator(), getTerminalPath());
        getTabPane().getTabs().add(terminalTab);
        getTabPane().getSelectionModel().select(terminalTab);
    }

    public void closeTerminal(ActionEvent... actionEvent) {
        ThreadHelper.runActionLater(() -> {
            final ObservableList<Tab> tabs = this.getTabPane().getTabs();
            if (tabs.size() == 1) {
                newTerminal(actionEvent);
            }
            tabs.remove(this);

            destroy();
        });
    }

    public void destroy() {
        ThreadHelper.start(() -> {
            while (Objects.isNull(getProcess())) {
                ThreadHelper.sleep(250);
            }
            getProcess().destroy();
            IOHelper.close(getInputReader(), getErrorReader(), getOutputWriter());
        });
    }

    public void onTerminalFxReady(Runnable onReadyAction) {
        terminal.onTerminalFxReady(onReadyAction);
    }

    public TabNameGenerator getTabNameGenerator() {
        return tabNameGenerator;
    }

    public Path getTerminalPath() {
        return terminal.getTerminalPath();
    }

    public TerminalConfig getTerminalConfig() {
        return terminal.getTerminalConfig();
    }

    public PtyProcess getProcess() {
        return terminal.getProcess();
    }

    public Reader getInputReader() {
        return terminal.getInputReader();
    }

    public Reader getErrorReader() {
        return terminal.getErrorReader();
    }

    public Writer getOutputWriter() {
        return terminal.getOutputWriter();
    }

    public Terminal getTerminal() {
        return terminal;
    }
}