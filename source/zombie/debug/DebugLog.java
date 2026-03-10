// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import pl.mjaron.tinyloki.ILogStream;
import pl.mjaron.tinyloki.Labels;
import pl.mjaron.tinyloki.StreamSet;
import pl.mjaron.tinyloki.TinyLoki;
import zombie.DebugFileWatcher;
import zombie.GameTime;
import zombie.PredicatedFileWatcher;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.statistics.StatisticManager;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.ui.UIDebugConsole;
import zombie.util.StringUtils;

/**
 * Created by LEMMYPC on 31/12/13.
 */
@UsedFromLua
public final class DebugLog {
    private static boolean initialized;
    public static boolean printServerTime;
    private static final DebugLog.OutputStreamWrapper s_stdout = new DebugLog.OutputStreamWrapper(System.out);
    private static final DebugLog.OutputStreamWrapper s_stderr = new DebugLog.OutputStreamWrapper(System.err);
    private static final PrintStream s_originalOut = new PrintStream(s_stdout, true);
    private static final PrintStream s_originalErr = new PrintStream(s_stderr, true);
    private static final PrintStream GeneralErr = new DebugLogStream(
        s_originalErr, s_originalErr, s_originalErr, new GeneralErrorDebugLogFormatter(), LogSeverity.All
    );
    private static ZLogger logFileLogger;
    private static PredicatedFileWatcher debugCfgFileWatcher;
    private static String debugCfgFileWatcherPath;
    private static boolean lokiInit;
    private static TinyLoki loki;
    private static StreamSet logSet;
    private static ILogStream errorStream;
    public static final DebugType Entity = DebugType.Entity;
    public static final DebugType General = DebugType.General;
    public static final DebugType DetailedInfo = DebugType.DetailedInfo;
    public static final DebugType Lua = DebugType.Lua;
    public static final DebugType MapLoading = DebugType.MapLoading;
    public static final DebugType Mod = DebugType.Mod;
    public static final DebugType Multiplayer = DebugType.Multiplayer;
    public static final DebugType Network = DebugType.Network;
    public static final DebugType NetworkFileDebug = DebugType.NetworkFileDebug;
    public static final DebugType Objects = DebugType.Objects;
    public static final DebugType Radio = DebugType.Radio;
    public static final DebugType Recipe = DebugType.Recipe;
    public static final DebugType Script = DebugType.Script;
    public static final DebugType Shader = DebugType.Shader;
    public static final DebugType Sound = DebugType.Sound;
    public static final DebugType Vehicle = DebugType.Vehicle;
    public static final DebugType Voice = DebugType.Voice;
    public static final DebugType Zombie = DebugType.Zombie;
    public static final DebugType Animal = DebugType.Animal;
    public static final DebugType CraftLogic = DebugType.CraftLogic;
    public static final DebugType Action = DebugType.Action;
    public static final DebugType Grapple = DebugType.Grapple;
    private static boolean logTraceFileLocationEnabled;
    private static PrintStream recordingOut;
    public static final int VERSION1 = 1;
    public static final int VERSION2 = 2;
    public static final int VERSION = 4;

    public static void setDefaultLogSeverity() {
        LogSeverity logSeverity = getDefaultLogSeverity();

        for (DebugType debugType : DebugType.values()) {
            if (logSeverity.ordinal() < debugType.getLogStream().getLogSeverity().ordinal()) {
                enableLog(debugType, logSeverity);
            }
        }
    }

    private static LogSeverity getDefaultLogSeverity() {
        if (Core.debug) {
            return LogSeverity.General;
        } else {
            return GameServer.server ? LogSeverity.Warning : LogSeverity.Off;
        }
    }

    public static void printLogLevels() {
        if (!GameServer.server) {
            DetailedInfo.trace("You can setup the log levels in the " + getConfigFileName() + " file");
        }

        DebugType.General.println("Logs configuration:");
        LogSeverity defaultLogSeverity = getDefaultLogSeverity();

        for (DebugType type : DebugType.values()) {
            DebugLogStream logStream = type.getLogStream();
            if (logStream.getLogSeverity() != defaultLogSeverity) {
                DebugType.General.println("%12s: %s", type.name(), logStream.getLogSeverity().name());
            }
        }

        DebugType.General.println("%12s: %s", "Default", defaultLogSeverity);
    }

    public static void enableLog(DebugType type, LogSeverity severity) {
        setLogSeverity(type, severity);
    }

    public static LogSeverity getLogLevel(DebugType type) {
        return getLogSeverity(type);
    }

    public static LogSeverity getLogSeverity(DebugType type) {
        return type.getLogStream().getLogSeverity();
    }

    public static void setLogSeverity(DebugType type, LogSeverity logSeverity) {
        type.getLogStream().setLogSeverity(logSeverity);
    }

    public static boolean isEnabled(DebugType type) {
        return type.isEnabled();
    }

    public static boolean isLogEnabled(DebugType type, LogSeverity logSeverity) {
        return type.getLogStream().isLogEnabled(logSeverity);
    }

    public static String formatString(DebugType type, LogSeverity logSeverity, Object affix, boolean allowRepeat, String formatNoParams) {
        return isLogEnabled(type, logSeverity) ? formatStringVarArgs(type, logSeverity, affix, allowRepeat, "%s", formatNoParams) : null;
    }

    public static String formatString(DebugType type, LogSeverity logSeverity, Object affix, boolean allowRepeat, String format, Object... params) {
        return isLogEnabled(type, logSeverity) ? formatStringVarArgs(type, logSeverity, affix, allowRepeat, format, params) : null;
    }

    public static String formatStringVarArgs(DebugType type, LogSeverity logSeverity, Object affix, boolean allowRepeat, String format, Object... params) {
        if (!isLogEnabled(type, logSeverity)) {
            return null;
        } else {
            String ms = generateCurrentTimeMillisStr();
            int frameNo = IsoWorld.instance.getFrameNo();
            String typeStr = StringUtils.leftJustify(type.toString(), 12);
            String formattedOutputStr = String.format(format, params);
            String affixedOutputStr = affix + formattedOutputStr;
            return !DebugLog.RepeatWatcher.check(type, logSeverity, affixedOutputStr, allowRepeat)
                ? null
                : logSeverity.logPrefix + typeStr + " f:" + frameNo + ", t:" + ms + "> " + affixedOutputStr;
        }
    }

    private static String generateCurrentTimeMillisStr() {
        String ms = String.valueOf(System.currentTimeMillis());
        if (GameServer.server || GameClient.client || printServerTime) {
            ms = ms + ", st:" + NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(GameTime.getServerTime()));
        }

        return ms;
    }

    public static void echoToLogFiles(LogSeverity logSeverity, String outString) {
        echoToLogFile(outString);
        echoToLoki(logSeverity, outString);
        echoToRecording(outString);
    }

    public static void echoExceptionLineToLogFiles(LogSeverity logSeverity, String messageType, String outString) {
        echoToLogFile(outString);
        echoExceptionLineToLoki(logSeverity, messageType, outString);
        echoToRecording(outString);
    }

    private static void echoToLoki(LogSeverity logSeverity, String formattedString) {
        if (logSet != null) {
            switch (logSeverity) {
                case Trace:
                case Noise:
                    logSet.verbose(formattedString);
                    break;
                case Debug:
                    logSet.debug(formattedString);
                    break;
                case General:
                    logSet.info(formattedString);
                    break;
                case Warning:
                    logSet.warning(formattedString);
                    break;
                case Error:
                    if (errorStream == null) {
                        errorStream = loki.stream().l("level", "error").open();
                    }

                    errorStream.log(formattedString);
                    break;
                default:
                    logSet.unknown(formattedString);
            }
        }
    }

    private static void echoExceptionLineToLoki(LogSeverity logSeverity, String messageType, String message) {
        if (logSet != null) {
            switch (logSeverity) {
                case Trace:
                case Noise:
                    logSet.verbose(message, Labels.of("type", messageType));
                    break;
                case Debug:
                    logSet.debug(message, Labels.of("type", messageType));
                    break;
                case General:
                    logSet.info(message, Labels.of("type", messageType));
                    break;
                case Warning:
                    logSet.warning(message, Labels.of("type", messageType));
                    break;
                case Error:
                    logSet.fatal().log(message, Labels.of("type", messageType));
                    break;
                default:
                    logSet.unknown(message, Labels.of("type", messageType));
            }
        }
    }

    private static void echoToLogFile(String formattedLine) {
        if (logFileLogger == null) {
            if (initialized) {
                return;
            }

            logFileLogger = new ZLogger(GameServer.server ? "DebugLog-server" : "DebugLog", false);
        }

        try {
            logFileLogger.writeUnsafe(formattedLine, null, false);
        } catch (Exception var2) {
            s_originalErr.println("Exception thrown writing to log file.");
            s_originalErr.println(var2);
            var2.printStackTrace(s_originalErr);
        }
    }

    private static void echoToRecording(String formattedString) {
        if (recordingOut != null) {
            int frameNo = IsoWorld.instance.getFrameNo();
            recordingOut.print(frameNo);
            recordingOut.print(",");
            recordingOut.print('"');
            recordingOut.print(formattedString);
            recordingOut.println('"');
        }
    }

    public static void log(DebugType type, String str) {
        type.println(str);
    }

    public static void setLogEnabled(DebugType type, boolean bEnabled) {
        DebugLogStream logStream = type.getLogStream();
        if (logStream.isEnabled() != bEnabled) {
            logStream.setLogSeverity(bEnabled ? getDefaultLogSeverity() : LogSeverity.Off);
        }
    }

    public static void log(String str) {
        log(DebugType.General, str);
    }

    public static ArrayList<DebugType> getDebugTypes() {
        ArrayList<DebugType> debugTypes = new ArrayList<>(Arrays.asList(DebugType.values()));
        debugTypes.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        return debugTypes;
    }

    public static void save() {
        LogSeverity[] logSeverityValues = LogSeverity.values();
        String[] logSeverityNames = new String[logSeverityValues.length];

        for (int i = 0; i < logSeverityValues.length; i++) {
            logSeverityNames[i] = logSeverityValues[i].name();
        }

        ArrayList<ConfigOption> options = new ArrayList<>();

        for (DebugType debugType : DebugType.values()) {
            StringConfigOption option = new StringConfigOption(debugType.name(), LogSeverity.Off.name(), logSeverityNames);
            option.setValue(getLogSeverity(debugType).name());
            options.add(option);
        }

        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 4, options);
    }

    private static String getConfigFileName() {
        return ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.ini";
    }

    public static void load() {
        String fileName = getConfigFileName();
        ConfigFile configFile = new ConfigFile();
        File file = new File(fileName);
        if (!file.exists()) {
            setDefaultLogSeverity();
            save();
        }

        if (configFile.read(fileName)) {
            if (configFile.getVersion() != 4) {
                setDefaultLogSeverity();
                save();
            } else {
                for (int i = 0; i < configFile.getOptions().size(); i++) {
                    ConfigOption configOption = configFile.getOptions().get(i);

                    try {
                        DebugType debugType = DebugType.valueOf(configOption.getName());
                        if (configFile.getVersion() == 1) {
                            setLogEnabled(debugType, StringUtils.tryParseBoolean(configOption.getValueAsString()));
                        } else {
                            LogSeverity logSeverity = LogSeverity.valueOf(configOption.getValueAsString());
                            setLogSeverity(debugType, logSeverity);
                        }
                    } catch (Exception var7) {
                    }
                }
            }
        }
    }

    public static boolean isLogTraceFileLocationEnabled() {
        return logTraceFileLocationEnabled;
    }

    public static PrintStream getRecordingOut() {
        return recordingOut;
    }

    public static void setRecordingOut(PrintStream recordingOut) {
        DebugLog.recordingOut = recordingOut;
    }

    public static DebugLogStream createLogStream(DebugType debugType) {
        return debugType.getLogStream() != null
            ? debugType.getLogStream()
            : new DebugLogStream(s_originalOut, s_originalOut, s_originalErr, new GenericDebugLogFormatter(debugType));
    }

    public static void setStdOut(OutputStream out) {
        s_stdout.setStream(out);
    }

    public static void setStdErr(OutputStream out) {
        s_stderr.setStream(out);
    }

    public static void init() {
        if (!initialized) {
            initialized = true;
            setStdOut(System.out);
            setStdErr(System.err);
            System.setOut(General.getLogStream());
            System.setErr(GeneralErr);
            if (!GameServer.server) {
                load();
            }

            logFileLogger = LoggerManager.getLogger(GameServer.server ? "DebugLog-server" : "DebugLog");
            if (!lokiInit) {
                lokiInit = true;
                String lokiUrl = System.getProperty("lokiUrl");
                if (lokiUrl != null) {
                    System.out.println("Loki logging enabled.");
                    String lokiUser = System.getProperty("lokiUser");
                    String lokiPass = System.getProperty("lokiPass");
                    loki = TinyLoki.withUrl(lokiUrl)
                        .withThreadExecutor(2000)
                        .withBasicAuth(lokiUser, lokiPass)
                        .withLabels(
                            Labels.of("instance", GameServer.server ? StatisticManager.getInstanceName() : GameClient.username)
                                .l("service_name", GameServer.server ? "pz.server" : "pz.client")
                        )
                        .open();
                    logSet = loki.streamSet().open();
                } else {
                    loki = null;
                    logSet = null;
                }
            }

            DebugType.General.getLogStream().setLogSeverity(LogSeverity.General);
            DebugType.Lua.getLogStream().setLogSeverity(LogSeverity.General);
            DebugType.Mod.getLogStream().setLogSeverity(LogSeverity.General);
            DebugType.Multiplayer.getLogStream().setLogSeverity(LogSeverity.General);
            DebugType.Network.getLogStream().setLogSeverity(LogSeverity.Error);
        }
    }

    public static void loadDebugConfig(String filepath) {
        if (!GameServer.server) {
            try {
                if (filepath == null) {
                    filepath = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.cfg";
                    File file = new File(filepath);
                    if (!file.exists() || !file.isFile()) {
                        return;
                    }
                }

                log("Attempting to read debug config...");
                File file = new File(filepath);
                if (!file.exists() || !file.isFile()) {
                    log("Attempting relative path...");
                    File p = new File("");
                    Path path = Path.of(p.toURI()).getParent();
                    file = new File(path + File.separator + filepath);
                }

                DetailedInfo.trace("file = " + file.getAbsolutePath());
                if (!file.exists() || !file.isFile()) {
                    log("Could not find debug config.");
                    return;
                }

                String selectedConfig = null;
                HashMap<String, ArrayList<String>> configs = new HashMap<>();
                HashMap<String, String> aliases = new HashMap<>();
                ArrayList<String> commands = null;
                boolean opened = false;
                BufferedReader br = new BufferedReader(new FileReader(file));

                try {
                    String line = null;

                    String l;
                    while ((l = br.readLine()) != null) {
                        String lastLine = line;
                        line = l.trim();
                        if (!line.startsWith("//") && !line.startsWith("#") && !StringUtils.isNullOrWhitespace(line)) {
                            if (line.startsWith("=")) {
                                selectedConfig = line.substring(1).trim();
                            } else if (line.startsWith("$")) {
                                try {
                                    String s = line.substring(1).trim();
                                    int i = s.indexOf(61);
                                    String alias = s.substring(0, i).trim();
                                    String command = s.substring(i + 1).trim();
                                    aliases.put(alias, command);
                                } catch (Exception var16) {
                                    var16.printStackTrace();
                                }
                            } else if (!opened && line.startsWith("{") && lastLine != null) {
                                opened = true;
                                commands = new ArrayList<>();
                                configs.put(lastLine, commands);
                            } else if (opened) {
                                if (line.startsWith("}")) {
                                    opened = false;
                                } else {
                                    commands.add(line);
                                }
                            }
                        }
                    }
                } catch (Throwable var17) {
                    try {
                        br.close();
                    } catch (Throwable var15) {
                        var17.addSuppressed(var15);
                    }

                    throw var17;
                }

                br.close();
                if (selectedConfig != null) {
                    if (selectedConfig.startsWith("$")) {
                        log("Selected debug alias = '" + selectedConfig + "'");
                        selectedConfig = aliases.get(selectedConfig.substring(1).trim());
                    } else {
                        log("Selected debug profile = '" + selectedConfig + "'");
                    }

                    String[] ss = selectedConfig.split("\\+");

                    for (String elem : ss) {
                        String profile = elem.trim();
                        if (configs.containsKey(profile)) {
                            log("Debug.cfg loading profile '" + profile + "'");

                            for (String s : configs.get(profile)) {
                                if (s.startsWith("+")) {
                                    readConfigCommand(s.substring(1), true);
                                } else if (s.startsWith("-")) {
                                    readConfigCommand(s.substring(1), false);
                                } else {
                                    log("unknown command: '" + s + "'");
                                }
                            }
                        } else {
                            log("Debug.cfg profile note found: '" + profile + "'");
                        }
                    }
                }

                startWatchingDebugCfgFile(file);
            } catch (Exception var18) {
                var18.printStackTrace();
            }
        }
    }

    private static void startWatchingDebugCfgFile(File file) {
        if (debugCfgFileWatcher == null || !debugCfgFileWatcherPath.equalsIgnoreCase(file.getPath())) {
            if (debugCfgFileWatcher != null) {
                stopWatchingDebugCfgFile();
            }

            String cfgFileDir = file.getParent();
            DebugFileWatcher.instance.addDirectory(cfgFileDir);
            debugCfgFileWatcherPath = file.getPath();
            debugCfgFileWatcher = new PredicatedFileWatcher(debugCfgFileWatcherPath, DebugLog::isDebugCfgPath, DebugLog::onDebugCfgFileChanged);
            DebugFileWatcher.instance.add(debugCfgFileWatcher);
        }
    }

    private static void stopWatchingDebugCfgFile() {
        DebugFileWatcher.instance.remove(debugCfgFileWatcher);
        debugCfgFileWatcher = null;
        debugCfgFileWatcherPath = null;
    }

    private static void onDebugCfgFileChanged(String path) {
        loadDebugConfig(debugCfgFileWatcherPath);
        printLogLevels();
    }

    private static boolean isDebugCfgPath(String path) {
        return StringUtils.equalsIgnoreCase(debugCfgFileWatcherPath, path);
    }

    private static void readConfigCommand(String s, boolean enable) {
        try {
            String logTypeStr = s;
            String logSeverityStr = null;
            if (StringUtils.containsWhitespace(s)) {
                String[] split = s.split("\\s+");
                logTypeStr = split[0].trim();
                logSeverityStr = split[1].trim();
            }

            LogSeverity logSeverity = LogSeverity.Debug;
            if (!StringUtils.isNullOrWhitespace(logSeverityStr)) {
                logSeverity = LogSeverity.valueOf(logSeverityStr);
            }

            if (logTypeStr.equalsIgnoreCase("LogTraceFileLocation")) {
                logTraceFileLocationEnabled = enable;
                return;
            }

            if (logTypeStr.equalsIgnoreCase("all")) {
                for (DebugType type : DebugType.values()) {
                    if (type != DebugType.General || enable) {
                        setLogSeverity(type, logSeverity);
                        setLogEnabled(type, enable);
                    }
                }

                return;
            }

            DebugType typex;
            if (logTypeStr.contains(".")) {
                String[] split = logTypeStr.split("\\.");
                typex = DebugType.valueOf(split[0]);
                ScriptType scriptType = ScriptType.valueOf(split[1]);
                ScriptManager.EnableDebug(scriptType, enable);
            } else {
                typex = DebugType.valueOf(logTypeStr);
            }

            setLogSeverity(typex, logSeverity);
            setLogEnabled(typex, enable);
        } catch (Exception var9) {
            General.printException(var9, "Exception thrown in readConfigCommand", LogSeverity.Error);
        }
    }

    public static void nativeLog(String logType, String logSeverity, String logTxt) {
        DebugType type = StringUtils.tryParseEnum(DebugType.class, logType, DebugType.General);
        LogSeverity severity = StringUtils.tryParseEnum(LogSeverity.class, logSeverity, LogSeverity.General);
        type.routedWrite(1, severity, logTxt);
    }

    private static final class OutputStreamWrapper extends FilterOutputStream {
        public OutputStreamWrapper(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.out.write(b, off, len);
            if (Core.debug && UIDebugConsole.instance != null && DebugOptions.instance.uiDebugConsoleDebugLog.getValue()) {
                UIDebugConsole.instance.addOutput(b, off, len);
            }
        }

        public void setStream(OutputStream out) {
            this.out = out;
        }
    }

    private static final class RepeatWatcher {
        private static final Object Lock = "RepeatWatcher_Lock";
        private static String lastLine;
        private static DebugType lastDebugType;
        private static LogSeverity lastLogSeverity;

        public static boolean check(DebugType type, LogSeverity logSeverity, String newLine, boolean allowRepeat) {
            synchronized (Lock) {
                if (allowRepeat) {
                    lastLine = null;
                    lastDebugType = null;
                    lastLogSeverity = null;
                    return true;
                } else if (lastLine == null) {
                    lastLine = newLine;
                    lastDebugType = type;
                    lastLogSeverity = logSeverity;
                    return true;
                } else if (lastDebugType == type && lastLogSeverity == logSeverity && lastLine.equals(newLine)) {
                    return false;
                } else {
                    lastLine = newLine;
                    lastDebugType = type;
                    lastLogSeverity = logSeverity;
                    return true;
                }
            }
        }
    }
}
