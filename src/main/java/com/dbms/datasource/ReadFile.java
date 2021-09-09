package com.dbms.datasource;

import com.dbms.presentation.IConsoleOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReadFile implements IReadFile{

    @Autowired
    private IConsoleOutput consoleOutput;

    @Autowired
    private Resource resource;

    @Override
    public JSONArray readJSONArrayFromFile(String filePath) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        try{
            FileReader reader = new FileReader(filePath);
            JSONArray userJsonArray = (JSONArray) jsonParser.parse(reader);
            return userJsonArray;
        } catch (FileNotFoundException e) {
            consoleOutput.error("ReadFile: readJSON: File not found. " + e);
            throw e;
        } catch (IOException e) {
            consoleOutput.error("ReadFile: readJSON: File read failed. " + e);
            throw e;
        } catch (ParseException e) {
            consoleOutput.error("ReadFile: readJSON: Imported file is not valid. Please make sure it is JSON format or if all fields are either boolean,string,array,object");
            throw e;
        }
    }

    public List<String> readAllFolders(String userGroup) throws IOException {
        List<String> output = new ArrayList<>();
        String dirName = resource.dbPath + userGroup;
        Path directoryPath = Paths.get(dirName);
        List<Path> filePathList = Files.list(directoryPath).collect(Collectors.toList());
        if(filePathList != null && !filePathList.isEmpty()) {
            for (Path path : filePathList) {
                String pathStr = path.toString();
                if(Files.isDirectory(path)){
                    String[] pathArr = pathStr.split("\\\\");
                    String dbName = pathArr[pathArr.length - 1];
                    output.add(dbName);
                }
            }
        }
        if(output.size() == 0){
            output.add("There are no database to show.");
        } else{
            output.add(0, "Below are the databases that the logged in user has access to: ");
        }
        return output;
    }

    @Override
    public Map<String, JSONArray> readFilesFromPath(String dirName, String userGroup) throws Exception {
        Map<String, JSONArray> files = null;

        try {
            dirName = resource.dbPath + userGroup + "\\" + dirName;
            Path directoryPath = Paths.get(dirName);
            List<Path> filePathList = Files.list(directoryPath).collect(Collectors.toList());
            if(filePathList != null && !filePathList.isEmpty()) {
                files = new HashMap<>();
                for (Path path : filePathList) {
                    String pathStr = path.toString();
                    if(!pathStr.contains("metadata.json")) {
                        String[] pathArr = pathStr.split("\\\\");
                        String fileNameWithExt = pathArr[pathArr.length - 1];
                        String[] fileNameArr = fileNameWithExt.split("\\.");
                        String fileName = fileNameArr[0];
                        JSONArray arr = readJSONArrayFromFile(pathStr);
                        files.put(fileName, arr);
                    }
                }
            }
        } catch (IOException e){
            consoleOutput.error("ReadFile: readFilesFromPath: IOException: "+e);
            throw e;
        } catch (Exception e){
            consoleOutput.error("ReadFile: readFilesFromPath: Exception: "+e);
            throw e;
        }
        return files;
    }

}
