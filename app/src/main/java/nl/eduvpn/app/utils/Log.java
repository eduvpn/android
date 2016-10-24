package nl.eduvpn.app.utils;

import nl.eduvpn.app.Constants;

/**
 * API for sending log output.
 * <p/>
 * Generally, use the Log.v() Log.d() Log.i() Log.w() and Log.e() methods.
 * <p/>
 * The order in terms of verbosity, from least to most is ERROR, WARN, INFO, DEBUG, VERBOSE.
 * Verbose should never be compiled into an application except during development. Debug logs are compiled in but stripped at runtime.
 * Error, warning and info logs are always kept.
 * <p/>
 * Created by Daniel Zolnai on 2016-09-14.
 */
public class Log {
    private static final boolean DEBUG = Constants.DEBUG;

    /**
     * Log an error.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void e(String tag, String message) {
        if (DEBUG) {
            android.util.Log.e(tag, message);
        }
    }

    /**
     * Log an error with an exception.
     *
     * @param tag       Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param message   The message to log.
     * @param exception The exception which was thrown, and should be logged.
     */
    public static void e(String tag, String message, Throwable exception) {
        if (DEBUG) {
            android.util.Log.e(tag, message, exception);
        }
    }

    /**
     * Log a warning.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void w(String tag, String message) {
        if (DEBUG) {
            android.util.Log.w(tag, message);
        }
    }

    /**
     * Log a warning.
     *
     * @param tag       Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param message   The message to log.
     * @param exception The exception which was thrown, and should be logged.
     */
    public static void w(String tag, String message, Throwable exception) {
        if (DEBUG) {
            android.util.Log.w(tag, message, exception);
        }
    }

    /**
     * Log an info message.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void i(String tag, String message) {
        if (DEBUG) {
            android.util.Log.i(tag, message);
        }
    }

    /**
     * Log a verbose message.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void v(String tag, String message) {
        if (DEBUG) {
            android.util.Log.v(tag, message);
        }
    }

    /**
     * Log a debug message.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void d(String tag, String message) {
        if (DEBUG) {
            android.util.Log.d(tag, message);
        }
    }
}