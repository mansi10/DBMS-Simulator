package com.dbms.presentation;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class DBMSLogger {

    private static Logger logger;

    private DBMSLogger(String className) {
        try {
            logger = Logger.getLogger(className);
            for (Handler handler : logger.getHandlers()) {  logger.removeHandler(handler); }

            ConsoleHandler consoleHandler = new ConsoleHandler();
            logger.addHandler(consoleHandler);
            logger.setLevel(Level.ALL);
            consoleHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("%s\n", record.getMessage());
                }
            });

            String logFile = Paths.get("").toAbsolutePath().toString() + "\\logs\\App_Logs.log\\";
            FileHandler fileHandler = new FileHandler(logFile, true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
                    return String.format("[%-15s] [%s] %s\n",
                            dateFormat.format(new Date(record.getMillis())),
                            record.getLevel(),
                            record.getMessage());
                }
            });
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Logger getLogger(String name) {
        if (logger == null) {
            new DBMSLogger(name);
        }
        return logger;
    }
}
