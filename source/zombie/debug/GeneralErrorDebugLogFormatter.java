// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

class GeneralErrorDebugLogFormatter implements IDebugLogFormatter {
    @Override
    public String format(LogSeverity logSeverity, String affix, boolean allowRepeat, String formatNoParams) {
        return DebugLog.formatString(DebugType.General, logSeverity, affix, allowRepeat, formatNoParams);
    }

    @Override
    public String format(LogSeverity logSeverity, String affix, boolean allowRepeat, String format, Object... params) {
        return DebugLog.formatString(DebugType.General, logSeverity, affix, allowRepeat, format, params);
    }
}
