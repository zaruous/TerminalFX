/**
 * 
 */
package com.kodedu.terminalfx;

/**
 * 
 */
public class Adapter {

	private ExplorerController explorerController;
	private FXMLController fxmlController;

	public void setExplorerController(ExplorerController explorerController) {
		this.explorerController = explorerController;
	}

	public void setFxmlController(FXMLController fxmlController) {
		this.fxmlController = fxmlController;
	}

	public ExplorerController getExplorerController() {
		return explorerController;
	}

	public FXMLController getFxmlController() {
		return fxmlController;
	}

}
