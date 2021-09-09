package com.dbms.service;

import com.dbms.datasource.Resource;
import com.dbms.presentation.ConsoleOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class ERDGenerator {

    @Autowired
    private Resource resource;

    @Autowired
    private ConsoleOutput consoleOutput;

    public ERDGenerator(){
        consoleOutput = new ConsoleOutput();
    }

    public String generateERD(String userGroup, String dbName) throws IOException, ParseException {
        generateJSON(userGroup, dbName);
        generateDotFile(userGroup, dbName);
        generateImgFile(userGroup, dbName);
        return "Generated ERD image.";
    }

    private void generateJSON(String userGroup, String dbName) throws IOException, ParseException {
        JSONObject output = new JSONObject();
        String path = resource.dbPath+userGroup+"\\"+dbName;
        String dirPath = path + "\\metadata.json";
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader(dirPath);
        JSONObject fullFile = (JSONObject) jsonParser.parse(reader);
        JSONArray tablesFromFile = (JSONArray) fullFile.get("tables");
        JSONObject tablesOutFile = new JSONObject();
        for(Object tableFromFileObj : tablesFromFile ){
            JSONObject tableFromFile = (JSONObject) tableFromFileObj;
            for(Object tableNameObj : tableFromFile.keySet()){
                String tableNameStr = (String) tableNameObj;
                JSONObject fullTableJson = (JSONObject) tableFromFile.get(tableNameStr);
                JSONObject columnsFromFile = (JSONObject) fullTableJson.get("columns");
                JSONObject columnsToFile = new JSONObject();
                for(Object columnObj : columnsFromFile.keySet()){
                    String columnName = (String) columnObj;
                    if(fullTableJson.get("primaryKey").equals(columnName)){
                        columnsToFile.put("*"+columnName, columnsFromFile.get(columnName));
                    } else{
                        columnsToFile.put(columnName, columnsFromFile.get(columnName));
                    }
                    tablesOutFile.put(tableNameStr, columnsToFile);
                }
            }
        }
        output.put("tables", tablesOutFile);
        System.out.println(output);
        String content = output.toString();
        content = content.substring(0, content.length()-2);
        content = content + "},\n" +
                "    \"relations\":[\n" +
                "    ],\n" +
                "    \"rankAdjustments\":\"\",\n" +
                "    \"label\":\"\"\n" +
                "}";
        String outpath = resource.erdPath+userGroup+"_"+dbName+".json";
        FileWriter fileWriter = new FileWriter(outpath);
        fileWriter.write(content);
        fileWriter.flush();
        fileWriter.close();
    }

    public void generateDotFile(String userGroup, String dbName) {
        String path = resource.erdPath;
        String anyCommand="erdot "+userGroup+"_"+dbName+".json";
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe", "/c", "cd "+path+" && "+anyCommand);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) { break; }
                consoleOutput.print(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateImgFile(String userGroup, String dbName) {
        String path = resource.erdPath;
        String anyCommand="dot "+userGroup+"_"+dbName+".dot -Tpng -o "+userGroup+"_"+dbName+".png";
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe", "/c", "cd "+path+" && "+anyCommand);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) { break; }
                consoleOutput.print(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
