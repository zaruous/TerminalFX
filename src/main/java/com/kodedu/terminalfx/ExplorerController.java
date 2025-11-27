package com.kodedu.terminalfx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import com.kodedu.terminalfx.config.FileManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class ExplorerController implements Initializable, Adapterable {

	@FXML
	private TreeTableView<File> tvExplorer;
	@FXML
	private TreeTableColumn<File, String> nameColumn;
	@FXML
	private TextField filterField;
	@FXML
	private Label filePathLabel;
	@FXML
	private TextArea fileContentArea;
	@FXML
	private Button newButton;
	@FXML
	private Button deleteButton;

	private final File rootFile = FileManager.getInstance().getRootFile();
	// 파일 내용 영역에 텍스트가 프로그램적으로 설정되었는지 여부를 나타냅니다.
	// 사용자 입력과 프로그램적 변경을 구분하여 리스너의 불필요한 트리거를 방지합니다.
	private boolean textSetProgrammatically = false;
	
	//텍스트 이디터 단축키 정의
	class TextEvent 
	{
		static final KeyCombination saveCombination = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
		static final KeyCombination exeuctionCombination = new KeyCodeCombination(KeyCode.F5);	
	}
	//트리뷰 단축키 정의
	class TreeEvent {
		static final KeyCombination renameBombination = new KeyCodeCombination(KeyCode.F2);
	}
	
	private Adapter adapter;
	ObjectProperty<File> currentFile = new SimpleObjectProperty<File>();
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		nameColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getName()));

		TreeItem<File> root = new LazyFileTreeItem(rootFile);
		root.setExpanded(true);
		tvExplorer.setRoot(root);
		tvExplorer.setShowRoot(true);

		
		/* 컨텍스트 메뉴 */
		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem newFolderMenuItem = new MenuItem("New Folder");
		newFolderMenuItem.setOnAction(e -> handleNewFolder());
		final MenuItem renameMenuItem = new MenuItem("Rename");
		renameMenuItem.setOnAction(e -> renameFile());

		tvExplorer.setContextMenu(contextMenu);
		tvExplorer.setOnContextMenuRequested(event -> {
			contextMenu.getItems().clear();
			TreeItem<File> selectedItem = tvExplorer.getSelectionModel().getSelectedItem();
			if (selectedItem != null) {
				if (selectedItem.getValue().isDirectory()) {
					contextMenu.getItems().add(newFolderMenuItem);
				} else {
					contextMenu.getItems().add(renameMenuItem);
				}
			}
		});
		/*트리뷰 이벤트 처리. */
		tvExplorer.addEventHandler(KeyEvent.KEY_PRESSED, this::tvExplorerOnKeyPress);
		tvExplorer.addEventFilter(MouseEvent.MOUSE_CLICKED, ev->{
			
			if(ev.getClickCount() == 2 && ev.getButton() == MouseButton.PRIMARY)
			{
				ev.consume();
				TreeItem<File> newValue = tvExplorer.getSelectionModel().getSelectedItem();
				if (newValue != null) {
					if(currentFile.get() == newValue.getValue())
					{
						return;
					}
					
					fileContentArea.setEditable(false);
					File selectedFile = newValue.getValue();
					filePathLabel.setText(selectedFile.getAbsolutePath());
					if (selectedFile.isFile()) {
						try {
							String content = new String(Files.readAllBytes(selectedFile.toPath()));
							textSetProgrammatically = true;
							fileContentArea.setText(content);
							textSetProgrammatically = false;
							fileContentArea.setEditable(true);
						} catch (IOException | OutOfMemoryError e) {
							fileContentArea.setText("Cannot display binary or very large file.");
						}
						currentFile.set(selectedFile);
					} else {
						fileContentArea.clear();
					}
				} else {
					filePathLabel.setText("No file selected");
					fileContentArea.clear();
				}
			}
			
		
		});

		filterField.textProperty().addListener((observable, oldValue, newValue) -> {
			String filter = (newValue == null) ? "" : newValue.trim().toLowerCase();
			if (filter.isEmpty()) {
				TreeItem<File> newRoot = new LazyFileTreeItem(rootFile);
				newRoot.setExpanded(true);
				tvExplorer.setRoot(newRoot);
			} else {
				TreeItem<File> filteredRoot = createFilteredTree(rootFile, filter);
				tvExplorer.setRoot(filteredRoot);
				if (filteredRoot != null) {
					expandAll(filteredRoot);
				}
			}
		});

		fileContentArea.textProperty().addListener((observable, oldValue, newValue) -> {
			if (textSetProgrammatically) {
				return;
			}
			TreeItem<File> selectedItem = tvExplorer.getSelectionModel().getSelectedItem();
			if (selectedItem != null && selectedItem.getValue().isFile()) {
				String currentLabel = filePathLabel.getText();
				if (!currentLabel.endsWith(" *")) {
					filePathLabel.setText(currentLabel + " *");
				}
			}
		});
		fileContentArea.setOnKeyPressed(event -> {
			if (TextEvent.saveCombination.match(event)) {
				saveFile();
				event.consume();
			}
			else if(TextEvent.exeuctionCombination.match(event))
			{
				adapter.getFxmlController().execute(  fileContentArea.getText() );
			}
		});
	}
	
	
	void tvExplorerOnKeyPress(KeyEvent ke) {
		
		if(TreeEvent.renameBombination.match(ke)) {
			ke.consume();
			renameFile();
		}
		
	}

	private void renameFile() {
		TreeItem<File> selectedItem = tvExplorer.getSelectionModel().getSelectedItem();
		if (selectedItem == null)
			return;
		File value = selectedItem.getValue();
		if (value == null || value.isDirectory() || !value.exists()) {
			return;
		}
		
		TextInputDialog dialog = new TextInputDialog(value.getName());
		dialog.setTitle("Rename File");
		dialog.setHeaderText("Enter the new file name. " + value.getAbsolutePath());
		dialog.setContentText("File name:");

		final File parentDir = value.getParentFile();

		dialog.showAndWait().ifPresent(fileName -> {
			if (fileName.isEmpty()) {
				showAlert("Invalid Name", "File name cannot be empty.");
				return;
			}

			File dest = new File(parentDir, fileName);
			if (value.renameTo(dest)) {
				selectedItem.setValue(dest);
			} else {
				showAlert("Error", "Could not rename file.");
			}

		});
	}
	
	private void handleNewFolder() {
		TreeItem<File> selectedItem = tvExplorer.getSelectionModel().getSelectedItem();

		File parentDir;
		TreeItem<File> parentItem;

		if (selectedItem == null) {
			showAlert("No Selection", "Please select a directory to create a new folder in.");
			return;
		}

		if (selectedItem.getValue().isDirectory()) {
			parentDir = selectedItem.getValue();
			parentItem = selectedItem;
		} else {
			parentDir = selectedItem.getValue().getParentFile();
			parentItem = selectedItem.getParent();
		}

		if (parentItem == null) {
			showAlert("Invalid Selection", "Cannot determine parent directory.");
			return;
		}

		TextInputDialog dialog = new TextInputDialog("newfolder");
		dialog.setTitle("New Folder");
dialog.setHeaderText("Enter the name for the new folder in\n" + parentDir.getAbsolutePath());
		dialog.setContentText("Folder name:");

		dialog.showAndWait().ifPresent(folderName -> {
			if (folderName.isEmpty()) {
				showAlert("Invalid Name", "Folder name cannot be empty.");
				return;
			}
			File newFolder = new File(parentDir, folderName);
			if (newFolder.mkdir()) {
				TreeItem<File> newItem = new LazyFileTreeItem(newFolder);
				parentItem.getChildren().add(newItem);
				// Sorting to keep folders on top
				parentItem.getChildren().sort(Comparator.comparing((TreeItem<File> ti) -> ti.getValue().isDirectory())
						.reversed().thenComparing(ti -> ti.getValue().getName()));
				parentItem.setExpanded(true); // Expand to show the new folder
			} else {
				showAlert("Error",
						"Could not create folder. It may already exist or you may not have permission.");
			}
		});
	}
	
	private void saveFile() {
		TreeItem<File> selectedItem = tvExplorer.getSelectionModel().getSelectedItem();
		if (selectedItem == null || selectedItem.getValue().isDirectory()) {
			showAlert("Save Error", "No file is selected or the selected item is a directory.");
			return;
		}

		File fileToSave = selectedItem.getValue();
		String content = fileContentArea.getText();

		try {
			Files.writeString(fileToSave.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			filePathLabel.setText(fileToSave.getAbsolutePath());
			showAlert("Success", "File saved successfully.");
		} catch (IOException e) {
			showAlert("Save Error", "Could not save file: " + e.getMessage());
		}
	}

	private TreeItem<File> createFilteredTree(File file, String filter) {
		TreeItem<File> item = new TreeItem<>(file);

		boolean isDirectory = file.isDirectory();
		boolean nameMatches = file.getName().toLowerCase().contains(filter);

		if (isDirectory) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File childFile : files) {
					TreeItem<File> childItem = createFilteredTree(childFile, filter);
					if (childItem != null) {
						item.getChildren().add(childItem);
					}
				}
			}
		}

		if ((isDirectory && (nameMatches || !item.getChildren().isEmpty())) || (!isDirectory && nameMatches)) {
			return item;
		} else {
			return null;
		}
	}

	private void expandAll(TreeItem<?> item) {
		if (item != null && !item.isLeaf()) {
			item.setExpanded(true);
			for (TreeItem<?> child : item.getChildren()) {
				expandAll(child);
			}
		}
	}

	@FXML
	private void handleNew() {
		TreeItem<File> selectedItem = tvExplorer.getSelectionModel().getSelectedItem();
		if (selectedItem == null) {
			showAlert("No Selection", "Please select a directory to create a file in.");
			return;
		}

		File selectedDir = selectedItem.getValue();
		// If a file is selected, use its parent directory
		if (!selectedDir.isDirectory()) {
			selectedItem = selectedItem.getParent();
			if (selectedItem == null) {
				showAlert("Invalid Selection", "Cannot determine directory.");
				return;
			}
			selectedDir = selectedItem.getValue();
		}


		TextInputDialog dialog = new TextInputDialog("newfile.txt");
		dialog.setTitle("New File");
		dialog.setHeaderText("Enter the name for the new file in\n" + selectedDir.getAbsolutePath());
		dialog.setContentText("File name:");

		final TreeItem<File> parentItem = selectedItem;
		final File parentDir = selectedDir;

		dialog.showAndWait().ifPresent(fileName -> {
			if (fileName.isEmpty()) {
				showAlert("Invalid Name", "File name cannot be empty.");
				return;
			}
			File newFile = new File(parentDir, fileName);
			try {
				if (newFile.createNewFile()) {
					TreeItem<File> newItem = new LazyFileTreeItem(newFile);
					parentItem.getChildren().add(newItem);
					// Sort children to maintain order
					parentItem.getChildren().sort(Comparator
							.comparing((TreeItem<File> ti) -> ti.getValue().isDirectory()).reversed()
							.thenComparing(ti -> ti.getValue().getName()));
				} else {
					showAlert("Error", "Could not create file. It may already exist.");
				}
			} catch (IOException e) {
				showAlert("Error", "An IO error occurred: " + e.getMessage());
			}
		});
	}

	@FXML
	private void handleDelete() {
		TreeItem<File> selectedItem = tvExplorer.getSelectionModel().getSelectedItem();
		if (selectedItem == null) {
			showAlert("No Selection", "Please select an item to delete.");
			return;
		}

		File selectedFile = selectedItem.getValue();
		if (selectedFile.equals(rootFile)) {
			showAlert("Cannot Delete", "Cannot delete the root directory.");
			return;
		}

		String type = selectedFile.isDirectory() ? "directory" : "file";
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Confirm Deletion");
		alert.setHeaderText("Delete " + type);
		alert.setContentText("Are you sure you want to delete this " + type + "?\n" + selectedFile.getAbsolutePath());

		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				try {
					if (selectedFile.isDirectory()) {
						// Recursively delete directory contents
						try (Stream<java.nio.file.Path> walk = Files.walk(selectedFile.toPath())) {
							walk.sorted(Comparator.reverseOrder())
									.map(java.nio.file.Path::toFile)
									.forEach(File::delete);
						}
					} else {
						Files.delete(selectedFile.toPath());
					}

					TreeItem<File> parent = selectedItem.getParent();
					if (parent != null) {
						parent.getChildren().remove(selectedItem);
					}
				} catch (IOException e) {
					showAlert("Error", "Failed to delete the " + type + ": " + e.getMessage());
				}
			}
		});
	}

	private void showAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	private class LazyFileTreeItem extends TreeItem<File> {
		private boolean isLeaf;
		private boolean isFirstTimeChildren = true;

		public LazyFileTreeItem(File file) {
			super(file);
		}

		@Override
		public ObservableList<TreeItem<File>> getChildren() {
			if (isFirstTimeChildren) {
				isFirstTimeChildren = false;
				super.getChildren().setAll(buildChildren(this));
			}
			return super.getChildren();
		}

		@Override
		public boolean isLeaf() {
			if (isFirstTimeChildren) {
				isLeaf = getValue().isFile();
			}
			return isLeaf;
		}

		private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> treeItem) {
			File f = treeItem.getValue();
			if (f != null && f.isDirectory()) {
				File[] files = f.listFiles();
				if (files != null) {
					ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
					Stream.of(files)
							.sorted(Comparator.comparing(File::isDirectory).reversed().thenComparing(File::getName))
							.forEach(file -> children.add(new LazyFileTreeItem(file)));
					return children;
				}
			}
			return FXCollections.emptyObservableList();
		}
	}

	@Override
	public void setAdapter(Adapter adapter) {
		this.adapter = adapter;
	}
	
	/**
	 * 저장되지않는 내용이 있는지 확인한다.
	 * @return
	 */
	public boolean isDirty() {
		return filePathLabel.getText().endsWith(" *");
	}
}
