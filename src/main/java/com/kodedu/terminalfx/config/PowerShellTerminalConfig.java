package com.kodedu.terminalfx.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import javafx.scene.paint.Color;

/**
 * Created by usta on 12.09.2016.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PowerShellTerminalConfig extends TerminalConfig {

	public PowerShellTerminalConfig() {
		super();
		setWindowsTerminalStarter("powershell.exe");
		setBackgroundColor(Color.rgb(16, 16, 16));
		setForegroundColor(Color.rgb(240, 240, 240));
		setCursorColor(Color.rgb(255, 0, 0, 0.5));
	}
}
