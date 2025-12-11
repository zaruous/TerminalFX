package com.kodedu.terminalfx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.SystemUtils;

import com.kodedu.terminalfx.annotation.WebkitCall;
import com.kodedu.terminalfx.config.TerminalConfig;
import com.kodedu.terminalfx.helper.ThreadHelper;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class Terminal extends TerminalView {

    private PtyProcess process;
    private final ObjectProperty<Writer> outputWriterProperty;
    private final Path terminalPath;
    private String[] termCommand;
    private final LinkedBlockingQueue<String> commandQueue;

    public Terminal() {
        this(null, null);
    }

    public Terminal(TerminalConfig terminalConfig, Path terminalPath) {
        setTerminalConfig(terminalConfig);
        this.terminalPath = terminalPath;
        outputWriterProperty = new SimpleObjectProperty<>();
        commandQueue = new LinkedBlockingQueue<>();
    }

    @WebkitCall
    public void command(String command) {
        try {
            commandQueue.put(command);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        ThreadHelper.start(() -> {
            try {
                final String commandToExecute = commandQueue.poll();
                getOutputWriter().write(commandToExecute);
                getOutputWriter().flush();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onTerminalReady() {
        ThreadHelper.start(() -> {
            try {
                initializeProcess();
            } catch (final Exception e) {
            }
        });
    }

    private void initializeProcess() throws Exception {
        final Path dataDir = getDataDir();
        if (SystemUtils.IS_OS_WINDOWS) {
            this.termCommand = getTerminalConfig().getWindowsTerminalStarter().split("\\s+");
        } else {
            this.termCommand = getTerminalConfig().getUnixTerminalStarter().split("\\s+");
        }

        final Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("TERM", "xterm");

        System.setProperty("PTY_LIB_FOLDER", dataDir.resolve("libpty").toString());

        if (Objects.nonNull(terminalPath) && Files.exists(terminalPath)) {
            this.process = exec(termCommand, envs, terminalPath.toString());
        } else {
            this.process = exec(termCommand, envs, getUserHome().toString());
        }

        columnsProperty().addListener(evt -> updateWinSize());
        rowsProperty().addListener(evt -> updateWinSize());
        updateWinSize();
        String defaultCharEncoding=System.getProperty("file.encoding");
        setInputReader(new BufferedReader(new InputStreamReader(process.getInputStream(), defaultCharEncoding)));
        setErrorReader(new BufferedReader(new InputStreamReader(process.getErrorStream(), defaultCharEncoding)));
        setOutputWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), defaultCharEncoding)));
        focusCursor();

        countDownLatch.countDown();

        process.waitFor();
    }
    
    /**
     * @param command
     * @param environment
     * @param workingDirectory
     * @return
     * @throws IOException
     */
    static PtyProcess exec(String[] command, Map<String, String> environment, String workingDirectory) throws IOException {
    	return exec(command, environment, workingDirectory, false, false, null);
    }
    
    /**
     * @param command
     * @param environment
     * @param workingDirectory
     * @param console
     * @param cygwin
     * @param logFile
     * @return
     * @throws IOException
     */
    static PtyProcess exec(String[] command, Map<String, String> environment, String workingDirectory, boolean console, boolean cygwin,
            File logFile) throws IOException {
    	PtyProcessBuilder builder = new PtyProcessBuilder(command)
    	        .setEnvironment(environment)
    	        .setDirectory(workingDirectory)
    	        .setConsole(console)
    	        .setCygwin(cygwin)
    	        .setLogFile(logFile);
    	    return builder.start();
    }

    private Path getDataDir() {
        final Path dataDir = getUserHome().resolve(".terminalfx");
        return dataDir;
    }

    private Path getUserHome() {
        final String userHome = System.getProperty("user.home");
        return Paths.get(userHome);
    }

    public Path getTerminalPath() {
        return terminalPath;
    }

    private void updateWinSize() {
        try {
            process.setWinSize(new WinSize(getColumns(), getRows()));
        } catch (Exception e) {
            //
        }
    }

    public ObjectProperty<Writer> outputWriterProperty() {
        return outputWriterProperty;
    }

    public Writer getOutputWriter() {
        return outputWriterProperty.get();
    }

    public void setOutputWriter(Writer writer) {
        outputWriterProperty.set(writer);
    }

    public PtyProcess getProcess() {
        return process;
    }

}
