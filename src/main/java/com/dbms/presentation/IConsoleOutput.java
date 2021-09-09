package com.dbms.presentation;

public interface IConsoleOutput {

    void info(String text);
    void warning(String text);
    void error(String text);
    void print(String text);
}
