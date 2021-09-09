package com.dbms.datasource;

import com.dbms.presentation.IConsoleOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class WriteFile implements IWriteFile{

    @Autowired
    private IConsoleOutput consoleOutput;

    @Autowired
    private Resource resource;

    public void writeFile(String content, String filePath) throws IOException {
        FileWriter fileWriter = null;
        try {
            filePath = resource.dbPath + filePath;
            fileWriter = new FileWriter(filePath);
            fileWriter.write(content);
        } catch (IOException e){
            consoleOutput.error("WriteFile: writeFile: IOException: "+e);
            throw e;
        } finally {
            if(fileWriter != null) {
                fileWriter.flush();
                fileWriter.close();
            }
        }

    }

    public void createDirectory(String dirName) throws IOException {
        Path path = Paths.get(resource.dbPath + dirName);
        try {
            if(!Files.exists(path)){
                Files.createDirectory(path);
            }
        } catch (IOException e){
            consoleOutput.error("WriteFile: createDirectory: IOException: "+e);
            throw e;
        }

    }


}
