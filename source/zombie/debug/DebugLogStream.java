// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.PrintStream;
import java.util.HashSet;
import zombie.core.Core;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public class DebugLogStream extends PrintStream {
    private LogSeverity logSeverity;
    private final PrintStream wrappedStream;
    private final PrintStream wrappedWarnStream;
    private final PrintStream wrappedErrStream;
    private final IDebugLogFormatter formatter;
    private static final int LEFT_JUSTIFY = 36;
    private final HashSet<String> debugOnceHashSet = new HashSet<>();

    public DebugLogStream(PrintStream out, PrintStream warn, PrintStream err, IDebugLogFormatter formatter) {
        this(out, warn, err, formatter, LogSeverity.Off);
    }

    public DebugLogStream(PrintStream out, PrintStream warn, PrintStream err, IDebugLogFormatter formatter, LogSeverity logSeverity) {
        super(out);
        this.wrappedStream = out;
        this.wrappedWarnStream = warn;
        this.wrappedErrStream = err;
        this.formatter = formatter;
        this.logSeverity = logSeverity;
    }

    public void setLogSeverity(LogSeverity newSeverity) {
        this.logSeverity = newSeverity;
    }

    public LogSeverity getLogSeverity() {
        return this.logSeverity;
    }

    public PrintStream getWrappedOutStream() {
        return this.wrappedStream;
    }

    public PrintStream getWrappedWarnStream() {
        return this.wrappedWarnStream;
    }

    public PrintStream getWrappedErrStream() {
        return this.wrappedErrStream;
    }

    public IDebugLogFormatter getFormatter() {
        return this.formatter;
    }

    protected void write(PrintStream out, LogSeverity logSeverity, String text) {
        if (this.isLogEnabled(logSeverity)) {
            String formattedString = this.formatter.format(logSeverity, "", true, text);
            if (formattedString != null) {
                out.print(formattedString);
                DebugLog.echoToLogFiles(logSeverity, formattedString);
            }
        }
    }

    protected void writeln(PrintStream out, LogSeverity logSeverity, String formatNoParams) {
        if (this.isLogEnabled(logSeverity)) {
            String formattedString = this.formatter.format(logSeverity, "", true, formatNoParams);
            if (formattedString != null) {
                out.println(formattedString);
                DebugLog.echoToLogFiles(logSeverity, formattedString);
            }
        }
    }

    protected void writeln(PrintStream out, LogSeverity logSeverity, String format, Object... params) {
        if (this.isLogEnabled(logSeverity)) {
            String formattedString = this.formatter.format(logSeverity, "", true, format, params);
            if (formattedString != null) {
                out.println(formattedString);
                DebugLog.echoToLogFiles(logSeverity, formattedString);
            }
        }
    }

    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, Object formatNoParams) {
        if (this.isLogEnabled(logSeverity)) {
            String callerAffix = generateCallerPrefix_Internal(backTraceOffset, 36, DebugLog.isLogTraceFileLocationEnabled(), "> ");
            String formattedString = this.formatter.format(logSeverity, callerAffix, allowRepeat, "%s", formatNoParams);
            if (!allowRepeat) {
                if (this.debugOnceHashSet.contains(callerAffix)) {
                    return;
                }

                this.debugOnceHashSet.add(callerAffix);
            }

            if (formattedString != null) {
                out.println(formattedString);
                DebugLog.echoToLogFiles(logSeverity, formattedString);
            }
        }
    }

    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, String format, Object... params) {
        if (this.isLogEnabled(logSeverity)) {
            String callerAffix = generateCallerPrefix_Internal(backTraceOffset, 36, DebugLog.isLogTraceFileLocationEnabled(), "> ");
            String formattedOutputStr = String.format(format, params);
            String formattedString = this.formatter.format(logSeverity, callerAffix, allowRepeat, formattedOutputStr);
            if (formattedString != null) {
                out.println(formattedString);
                DebugLog.echoToLogFiles(logSeverity, formattedString);
            }
        }
    }

    private void writeln(PrintStream out, String formatNoParams) {
        this.writeln(out, LogSeverity.General, formatNoParams);
    }

    private void writeln(PrintStream out, String format, Object... params) {
        this.writeln(out, LogSeverity.General, format, params);
    }

    /**
     * Returns the class name and method name prefix of the calling code.
     */
    public static String generateCallerPrefix() {
        return generateCallerPrefix_Internal(1, 0, DebugLog.isLogTraceFileLocationEnabled(), "");
    }

    private static String generateCallerPrefix_Internal(int backTraceOffset, int leftJustify, boolean includeLogTraceFileLocation, String suffix) {
        StackTraceElement stackTraceElement = tryGetCallerTraceElement(4 + backTraceOffset);
        if (stackTraceElement == null) {
            return StringUtils.leftJustify("(UnknownStack)", leftJustify) + suffix;
        } else {
            String stackTraceElementString = getStackTraceElementString(stackTraceElement, includeLogTraceFileLocation);
            return leftJustify <= 0 ? stackTraceElementString + suffix : StringUtils.leftJustify(stackTraceElementString, leftJustify) + suffix;
        }
    }

    public static StackTraceElement tryGetCallerTraceElement(int depthIdx) {
        try {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            return stackTraceElements.length <= depthIdx ? null : stackTraceElements[depthIdx];
        } catch (SecurityException var2) {
            return null;
        }
    }

    public static String getStackTraceElementString(StackTraceElement stackTraceElement, boolean includeLogTraceFileLocation) {
        if (stackTraceElement == null) {
            return "(UnknownStack)";
        } else {
            String classNameOnly = getUnqualifiedClassName(stackTraceElement.getClassName());
            String methodName = stackTraceElement.getMethodName();
            String comment;
            if (stackTraceElement.isNativeMethod()) {
                comment = " (Native Method)";
            } else if (includeLogTraceFileLocation) {
                int lineNo = stackTraceElement.getLineNumber();
                String fileName = stackTraceElement.getFileName();
                comment = String.format("(%s:%d)", fileName, lineNo);
            } else {
                comment = "";
            }

            return classNameOnly + "." + methodName + comment;
        }
    }

    public static String getTopStackTraceString(Throwable ex) {
        if (ex == null) {
            return "Null Exception";
        } else {
            StackTraceElement[] stackTrace = ex.getStackTrace();
            if (stackTrace != null && stackTrace.length != 0) {
                StackTraceElement topElement = stackTrace[0];
                return getStackTraceElementString(topElement, true);
            } else {
                return "No Stack Trace Available";
            }
        }
    }

    public void printStackTrace(LogSeverity severity, int depthStart, int depthCount, String messageFormat, Object... params) {
        if (this.isLogEnabled(severity)) {
            PrintStream outStream = this.getPrintStream(severity);
            if (messageFormat != null) {
                String message = !PZArrayUtil.isNullOrEmpty(params) ? String.format(messageFormat, params) : messageFormat;
                outStream.println(message);
                DebugLog.echoExceptionLineToLogFiles(LogSeverity.Error, "StackTraceMessage", message);
            }

            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            String stackTraceString = StackTraceContainer.getStackTraceString(stackTraceElements, "\t", depthStart + 2, depthCount);
            outStream.println(stackTraceString);
            DebugLog.echoExceptionLineToLogFiles(LogSeverity.Error, "StackTrace", stackTraceString);
        }
    }

    private PrintStream getPrintStream(LogSeverity severity) {
        return switch (severity) {
            case Trace, Noise, Debug, General -> this.wrappedStream;
            case Warning -> this.wrappedWarnStream;
            case Error -> this.wrappedErrStream;
            default -> {
                this.error("Unhandled LogSeverity: %s. Defaulted to Error.", String.valueOf(severity));
                yield this.wrappedErrStream;
            }
        };
    }

    private static String getUnqualifiedClassName(String className) {
        String classNameOnly = className;
        int lastIndexOf = className.lastIndexOf(46);
        if (lastIndexOf > -1 && lastIndexOf < className.length() - 1) {
            classNameOnly = className.substring(lastIndexOf + 1);
        }

        return classNameOnly;
    }

    public boolean isEnabled() {
        return this.getLogSeverity() != LogSeverity.Off;
    }

    public boolean isLogEnabled(LogSeverity logSeverity) {
        return this.isEnabled() && logSeverity.ordinal() >= this.getLogSeverity().ordinal();
    }

    public void trace(Object formatNoParams) {
        this.trace(1, formatNoParams);
    }

    public void trace(String format, Object... params) {
        this.trace(1, format, params);
    }

    public void debugln(Object formatNoParams) {
        this.debugln(1, formatNoParams);
    }

    public void debugln(String format, Object... params) {
        this.debugln(1, format, params);
    }

    public void debugOnceln(Object formatNoParams) {
        this.debugOnceln(1, formatNoParams);
    }

    public void debugOnceln(String format, Object... params) {
        this.debugOnceln(1, format, params);
    }

    public void noise(Object formatNoParams) {
        this.noise(1, formatNoParams);
    }

    public void noise(String format, Object... params) {
        this.noise(1, format, params);
    }

    /**
     * Prints an object to the Warning stream.  The string produced by the String.valueOf(Object) method is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param formatNoParams The Object to be printed
     */
    public void warn(Object formatNoParams) {
        this.warn(1, formatNoParams);
    }

    public void warn(String format, Object... params) {
        this.warn(1, format, params);
    }

    public void warnOnce(Object formatNoParams) {
        this.warnOnce(1, formatNoParams);
    }

    public void warnOnce(String format, Object... params) {
        this.warnOnce(1, format, params);
    }

    /**
     * Prints an object to the Error stream.  The string produced by the String.valueOf(Object) method is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param formatNoParams The Object to be printed
     */
    public void error(Object formatNoParams) {
        this.error(1, formatNoParams);
    }

    public void error(String format, Object... params) {
        this.error(1, format, params);
    }

    public void debugln(int backTraceOffset, Object formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, true, formatNoParams);
        }
    }

    public void debugln(int backTraceOffset, String format, Object... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, true, format, params);
        }
    }

    public void debugOnceln(int backTraceOffset, Object formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, false, formatNoParams);
        }
    }

    public void debugOnceln(int backTraceOffset, String format, Object... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, false, format, params);
        }
    }

    public void noise(int backTraceOffset, Object formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Noise, backTraceOffset + 1, true, formatNoParams);
        }
    }

    public void noise(int backTraceOffset, String format, Object... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Noise, backTraceOffset + 1, true, format, params);
        }
    }

    public void warn(int backTraceOffset, Object formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, true, formatNoParams);
    }

    public void warn(int backTraceOffset, String format, Object... params) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, true, format, params);
    }

    public void error(int backTraceOffset, Object formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedErrStream, LogSeverity.Error, backTraceOffset + 1, true, formatNoParams);
    }

    public void error(int backTraceOffset, String format, Object... params) {
        this.writeWithCallerPrefixln(this.wrappedErrStream, LogSeverity.Error, backTraceOffset + 1, true, format, params);
    }

    public void trace(int backTraceOffset, Object formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Trace, backTraceOffset + 1, true, formatNoParams);
    }

    public void trace(int backTraceOffset, String format, Object... params) {
        this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Trace, backTraceOffset + 1, true, format, params);
    }

    public void warnOnce(int backTraceOffset, Object formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, false, formatNoParams);
    }

    public void warnOnce(int backTraceOffset, String format, Object... params) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, false, format, params);
    }

    /**
     * Prints a boolean value.  The string produced by String.valueOf(boolean) is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param b The boolean to be printed
     */
    @Override
    public void print(boolean b) {
        this.write(this.wrappedStream, LogSeverity.General, b ? "true" : "false");
    }

    /**
     * Prints a character.  The character is translated into one or more bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param c The char to be printed
     */
    @Override
    public void print(char c) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(c));
    }

    /**
     * Prints an integer.  The string produced by String.valueOf(int) is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param i The int to be printed
     */
    @Override
    public void print(int i) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(i));
    }

    /**
     * Prints a long integer.  The string produced by String.valueOf(long) is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param l The long to be printed
     */
    @Override
    public void print(long l) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(l));
    }

    /**
     * Prints a floating-point number.  The string produced by String.valueOf(float) is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param f The float to be printed
     */
    @Override
    public void print(float f) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(f));
    }

    /**
     * Prints a double-precision floating-point number.  The string produced by
     *  String.valueOf(double) is translated into
     *  bytes according to the platform's default character encoding, and these
     *  bytes are written in exactly the manner of the PrintStream.write(int) method.
     * 
     * @param d The double to be printed
     */
    @Override
    public void print(double d) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(d));
    }

    /**
     * Prints a string.  If the argument is null then the string
     *  "null" is printed.  Otherwise, the string's characters are
     *  converted into bytes according to the platform's default character
     *  encoding, and these bytes are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param s The String to be printed
     */
    @Override
    public void print(String s) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(s));
    }

    /**
     * Prints an object.  The string produced by the String.valueOf(Object) method is translated into bytes
     *  according to the platform's default character encoding, and these bytes
     *  are written in exactly the manner of the
     *  PrintStream.write(int) method.
     * 
     * @param obj The Object to be printed
     */
    @Override
    public void print(Object obj) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(obj));
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        this.write(this.wrappedStream, LogSeverity.General, String.format(format, args));
        return this;
    }

    /**
     * Terminates the current line by writing the line separator string.  The
     *  line separator string is defined by the system property
     *  line.separator, and is not necessarily a single newline
     *  character ('\n').
     */
    @Override
    public void println() {
        this.writeln(this.wrappedStream, "");
    }

    /**
     * Prints a boolean and then terminate the line.  This method behaves as
     *  though it invokes print(boolean) and then
     *  println().
     * 
     * @param x The boolean to be printed
     */
    @Override
    public void println(boolean x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a character and then terminate the line.  This method behaves as
     *  though it invokes print(char) and then
     *  println().
     * 
     * @param x The char to be printed.
     */
    @Override
    public void println(char x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints an integer and then terminate the line.  This method behaves as
     *  though it invokes print(int) and then
     *  println().
     * 
     * @param x The int to be printed.
     */
    @Override
    public void println(int x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a long and then terminate the line.  This method behaves as
     *  though it invokes print(long) and then
     *  println().
     * 
     * @param x a The long to be printed.
     */
    @Override
    public void println(long x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a float and then terminate the line.  This method behaves as
     *  though it invokes print(float) and then
     *  println().
     * 
     * @param x The float to be printed.
     */
    @Override
    public void println(float x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a double and then terminate the line.  This method behaves as
     *  though it invokes print(double) and then
     *  println().
     * 
     * @param x The double to be printed.
     */
    @Override
    public void println(double x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a character and then terminate the line.  This method behaves as
     *  though it invokes print(char) and then
     *  println().
     * 
     * @param x The char to be printed.
     */
    @Override
    public void println(char[] x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    /**
     * Prints a String and then terminate the line.  This method behaves as
     *  though it invokes print(String) and then
     *  println().
     * 
     * @param x The String to be printed.
     */
    @Override
    public void println(String x) {
        this.writeln(this.wrappedStream, x);
    }

    /**
     * Prints an Object and then terminate the line.  This method calls
     *  at first String.valueOf(x) to get the printed object's string value,
     *  then behaves as
     *  though it invokes print(String) and then
     *  println().
     * 
     * @param x The Object to be printed.
     */
    @Override
    public void println(Object x) {
        this.writeln(this.wrappedStream, "%s", x);
    }

    public void println(String format, Object... params) {
        this.writeln(this.wrappedStream, LogSeverity.General, format, params);
    }

    public void printException(Throwable ex, String errorMessage, LogSeverity severity) {
        this.printException(ex, errorMessage, generateCallerPrefix(), severity);
    }

    public void printException(Throwable ex, String errorMessage, String callerPrefix, LogSeverity severity) {
        if (ex == null) {
            this.warn("Null exception passed.");
        } else if (this.isLogEnabled(severity)) {
            PrintStream outStream = this.getPrintStream(severity);
            boolean includeStack = this.shouldIncludeStackTrace(severity);
            if (includeStack) {
                StringBuilder sb = new StringBuilder();
                if (errorMessage != null) {
                    sb.append(
                        String.format(
                            "%s> Exception thrown%s\t%s at %s. Message: %s", callerPrefix, System.lineSeparator(), ex, getTopStackTraceString(ex), errorMessage
                        )
                    );
                } else {
                    sb.append(String.format("%s> Exception thrown%s\t%s at %s.", callerPrefix, System.lineSeparator(), ex, getTopStackTraceString(ex)));
                }

                sb.append(System.lineSeparator());
                StackTraceContainer.getStackTraceString(sb, ex, "Stack trace:", "\t", 0, -1);
                this.write(outStream, severity, sb.toString());
            } else if (errorMessage != null) {
                String message = String.format("%s> Exception thrown %s at %s. Message: %s", callerPrefix, ex, getTopStackTraceString(ex), errorMessage);
                this.writeln(outStream, severity, message);
            } else {
                String message = String.format("%s> Exception thrown %s at %s.", callerPrefix, ex, getTopStackTraceString(ex));
                this.writeln(outStream, severity, message);
            }
        }
    }

    private boolean shouldIncludeStackTrace(LogSeverity severity) {
        return switch (severity) {
            case Trace, Noise, General, Warning -> false;
            default -> true;
        };
    }

    public void printException(Throwable ex, LogSeverity severity, String callerPrefix, String errorMessageFormat, Object... params) {
        if (this.isLogEnabled(severity)) {
            String errorMessage = String.format(errorMessageFormat, params);
            this.printException(ex, errorMessage, callerPrefix, severity);
        }
    }
}
