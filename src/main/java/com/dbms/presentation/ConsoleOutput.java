package com.dbms.presentation;

import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class ConsoleOutput implements IConsoleOutput{

    Logger logger = DBMSLogger.getLogger(ConsoleOutput.class.getName());

    @Override
    public void info(String text) {
        logger.info(text);
    }

    @Override
    public void warning(String text) {
        logger.warning(text);
    }

    @Override
    public void error(String text) {
        logger.severe(text);
    }

    @Override
    public void print(String text) {
        System.out.println(text);
    }
}
