package com.kodedu.terminalfx;

import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import com.kodedu.terminalfx.config.PowerShellTerminalConfig;
import com.kodedu.terminalfx.config.PropertiesUtil;
import com.kodedu.terminalfx.config.TerminalConfig;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

public class FXMLController implements Initializable {

	@FXML
	private TextArea txtCommand;
	@FXML
    public TabPane tabPane;
	Properties prop;
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    	
//    	super.initialize(url, rb);
    	miNewTabOnAction();
    	this.prop = PropertiesUtil.createOrLoad(TerminalView.class, ()->{
			return PropertiesUtil.of(Map.of("content",""));}
		);
    	
    	Platform.runLater(()->{
    		this.txtCommand.setText(prop.getProperty("content", ""));	
    	});
    }

    @FXML
    public void miNewTabOnAction() {

    	
//      Dark Config
      TerminalConfig darkConfig = new TerminalConfig();
      darkConfig.setBackgroundColor(Color.rgb(16, 16, 16));
      darkConfig.setForegroundColor(Color.rgb(240, 240, 240));
      darkConfig.setCursorColor(Color.rgb(255, 0, 0, 0.5));

//      CygWin Config
//      TerminalConfig cygwinConfig = new CygwinlTerminalConfig();

      //powershell Config
      TerminalConfig powershellConfig = new PowerShellTerminalConfig();
      
//      Default Config
//      TerminalConfig defaultConfig = new TerminalConfig();


      TerminalBuilder terminalBuilder = new TerminalBuilder(powershellConfig);
      TerminalTab terminal = terminalBuilder.newTerminal();
      

      tabPane.getTabs().add(terminal);
    }
    
    @FXML
    public void btnExecOnAction() {
    	Tab selectedItem = tabPane.getSelectionModel().getSelectedItem();
    	if(selectedItem == null) return;
    	TerminalTab t = (TerminalTab)selectedItem;
    	String[] split = txtCommand.getText().split("\n");
    	for (String cmd : split) {
			t.getTerminal().command(cmd + "\r");
    	}
    }
    
    @FXML
    public void btnSaveOnAction() {
		this.prop.setProperty("content", this.txtCommand.getText());
		PropertiesUtil.save(TerminalView.class, this.prop, (e)->{
			e.printStackTrace();
		});
	}

}
