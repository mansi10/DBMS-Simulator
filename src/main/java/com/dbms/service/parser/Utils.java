package com.dbms.service.parser;

import com.dbms.datasource.IWriteFile;
import com.dbms.presentation.IConsoleOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Utils {

    @Autowired
    private IWriteFile writeFile;

    @Autowired
    private IConsoleOutput logger;

    public void updateTableFile(String text, String fileName) {
        try {
            writeFile.writeFile(text, fileName);
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
        }
    }
}
