package com.dbms.presentation;

import org.springframework.stereotype.Component;

import java.util.Scanner;
import java.util.logging.Logger;

@Component
public class ReadUserInput implements IReadUserInput {

    private Scanner scanner;

    Logger logger = DBMSLogger.getLogger(ConsoleOutput.class.getName());

    public ReadUserInput(){
        scanner = new Scanner(System.in);
    }

    @Override
    public String getStringInput(String input){
        logger.info(input);
        return scanner.nextLine();
    }

    @Override
    public int getIntInput(String input){
        logger.info(input);
        return scanner.nextInt();
    }

}
