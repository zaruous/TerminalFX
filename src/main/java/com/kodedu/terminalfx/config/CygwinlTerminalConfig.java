package com.kodedu.terminalfx.config;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CygwinlTerminalConfig extends TerminalConfig {

	public CygwinlTerminalConfig() {
		super();
		setWindowsTerminalStarter("C:\\cygwin64\\bin\\bash -i");
		setFontSize(14);
	}

}
