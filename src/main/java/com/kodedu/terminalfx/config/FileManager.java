/**
 * 
 */
package com.kodedu.terminalfx.config;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import com.kodedu.terminalfx.TerminalView;

/**
 * 
 */
public class FileManager {
	Properties prop;
	private static FileManager INSTANCE = null;
	File scriptDir;
	private FileManager() {
		this.prop = PropertiesUtil.createOrLoad(TerminalView.class, ()->{
			return PropertiesUtil.of(Map.of("manageDir",".script"));}
		);
		String property = this.prop.getProperty("manageDir", ".script");
		if(property == null || property.isBlank())
			property = ".script";
		scriptDir = new File(property);
		if(!scriptDir.exists())scriptDir.mkdirs();
	}
	
	public static synchronized FileManager getInstance() {
		if(INSTANCE == null)
			INSTANCE = new FileManager();
		return INSTANCE;
	}
	
	public File getRootFile() {
		return this.scriptDir;
	}
	
	
}
