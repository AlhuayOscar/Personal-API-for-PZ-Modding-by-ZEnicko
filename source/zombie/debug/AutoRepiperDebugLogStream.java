// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.PrintStream;
import java.util.Locale;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class AutoRepiperDebugLogStream extends DebugLogStream {
    private final DebugType defaultDebugType;

    public AutoRepiperDebugLogStream(DebugType defaultOut, DebugType defaultDebugType, LogSeverity logSeverity) {
        super(defaultOut.getLogStream(), null, null, null, logSeverity);
        this.defaultDebugType = defaultDebugType;
    }

    public DebugType getDefaultDebugType() {
        return this.defaultDebugType;
    }

    public AutoRepiperDebugLogStream.RepiperPacket parseRepiper(Object object, LogSeverity defaultLogSeverity) {
        AutoRepiperDebugLogStream.RepiperPacket repiper = AutoRepiperDebugLogStream.RepiperPacket.alloc(object, defaultLogSeverity, this.getDefaultDebugType());
        this.parseRepipeDirection(repiper);
        this.parseRepipedLogSeverity(repiper);
        return repiper;
    }

    protected void parseRepipeDirection(AutoRepiperDebugLogStream.RepiperPacket repiper) {
        if (repiper.getParsedObject() instanceof String text) {
            int indexOfColon = text.indexOf(58);
            if (indexOfColon > 0) {
                String debugTypeStr = text.substring(0, indexOfColon);
                if (debugTypeStr.indexOf(10) <= -1 && debugTypeStr.indexOf(32) <= -1 && debugTypeStr.indexOf(9) <= -1) {
                    for (DebugType debugType : DebugType.values()) {
                        if (debugType.name().equalsIgnoreCase(debugTypeStr)) {
                            repiper.repipeDirection = debugType;
                            repiper.parsedText = text.substring(indexOfColon + 1);
                            break;
                        }
                    }
                }
            }
        }
    }

    protected void parseRepipedLogSeverity(AutoRepiperDebugLogStream.RepiperPacket repiper) {
        if (repiper.getParsedObject() instanceof String text) {
            int startAt = 0;

            for (int i = 0; i < 2; i++) {
                int indexOfColon = text.indexOf(58, startAt);
                if (indexOfColon <= 0) {
                    break;
                }

                String logSeverityStr = text.substring(startAt, indexOfColon);
                LogSeverity parsedLogSeverity = this.parseRepipedLogSeverityExact(logSeverityStr);
                if (parsedLogSeverity != null) {
                    repiper.logSeverity = parsedLogSeverity;
                    repiper.parsedText = text.substring(indexOfColon + 1);
                    return;
                }

                startAt = indexOfColon + 1;
            }
        }
    }

    private LogSeverity parseRepipedLogSeverityExact(String logSeverityStr) {
        String var2 = logSeverityStr.toUpperCase(Locale.ROOT);

        return switch (var2) {
            case "TRACE" -> LogSeverity.Trace;
            case "NOISE" -> LogSeverity.Noise;
            case "DEBUG" -> LogSeverity.Debug;
            case "WARN" -> LogSeverity.Warning;
            case "ERROR" -> LogSeverity.Error;
            default -> null;
        };
    }

    protected PrintStream getRepipedStream(PrintStream stream, DebugType repipedTo) {
        return this.getRepipedStream(stream, repipedTo.getLogStream());
    }

    protected PrintStream getRepipedStream(PrintStream stream, DebugLogStream repipedTo) {
        if (stream == this.getWrappedOutStream()) {
            return repipedTo.getWrappedOutStream();
        } else if (stream == this.getWrappedWarnStream()) {
            return repipedTo.getWrappedWarnStream();
        } else {
            return stream == this.getWrappedErrStream() ? repipedTo.getWrappedErrStream() : repipedTo.getWrappedOutStream();
        }
    }

    @Override
    protected void write(PrintStream out, LogSeverity logSeverity, String text) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(text, logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().write(repipedOutStream, repipedLogSeverity, text);
        }
    }

    @Override
    protected void writeln(PrintStream out, LogSeverity logSeverity, String formatNoParams) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(formatNoParams, logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().writeln(repipedOutStream, repipedLogSeverity, repiperPacket.getParsedString());
        }
    }

    @Override
    protected void writeln(PrintStream out, LogSeverity logSeverity, String format, Object... params) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(format, logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().writeln(repipedOutStream, repipedLogSeverity, repiperPacket.getParsedString(), params);
        }
    }

    @Override
    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, Object formatNoParams) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(formatNoParams, logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream()
                .writeWithCallerPrefixln(repipedOutStream, repipedLogSeverity, backTraceOffset + 1, allowRepeat, repiperPacket.getParsedString());
        }
    }

    @Override
    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, String format, Object... params) {
        try (AutoRepiperDebugLogStream.RepiperPacket repiperPacket = this.parseRepiper(format, logSeverity)) {
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream()
                .writeWithCallerPrefixln(repipedOutStream, repipedLogSeverity, backTraceOffset, allowRepeat, repiperPacket.getParsedString(), params);
        }
    }

    public static class RepiperPacket extends PooledObject implements AutoCloseable {
        private String parsedText;
        private Object inObject;
        private LogSeverity logSeverity;
        private DebugType repipeDirection;
        private static final Pool<AutoRepiperDebugLogStream.RepiperPacket> s_pool = new Pool<>(AutoRepiperDebugLogStream.RepiperPacket::new);

        private RepiperPacket() {
        }

        @Override
        public void onReleased() {
            this.parsedText = null;
            this.inObject = null;
            this.logSeverity = null;
            this.repipeDirection = null;
        }

        public Object getParsedObject() {
            return this.parsedText != null ? this.parsedText : this.inObject;
        }

        public String getParsedString() {
            return this.parsedText != null ? this.parsedText : String.valueOf(this.inObject);
        }

        public static AutoRepiperDebugLogStream.RepiperPacket alloc(Object object, LogSeverity defaultLogSeverity, DebugType defaultDebugType) {
            AutoRepiperDebugLogStream.RepiperPacket newInstance = s_pool.alloc();
            newInstance.parsedText = null;
            newInstance.inObject = object;
            newInstance.logSeverity = defaultLogSeverity;
            newInstance.repipeDirection = defaultDebugType;
            return newInstance;
        }

        @Override
        public void close() {
            Pool.tryRelease(this);
        }
    }
}
