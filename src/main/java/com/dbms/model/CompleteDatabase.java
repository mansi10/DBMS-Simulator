package com.dbms.model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.Map;

public class CompleteDatabase {

    private Map<String, JSONArray> tableRecords;
    private JSONObject metaData;
    private String dbName;
    private String dbPath = Paths.get("").toAbsolutePath().toString() + "\\data\\";

    public CompleteDatabase(){
    	
    }

    public Map<String, JSONArray> getTableRecords() {
        return tableRecords;
    }

    public void setTableRecords(Map<String, JSONArray> tableRecords) {
        this.tableRecords = tableRecords;
    }

    public JSONObject getMetaData() {
        return metaData;
    }

    public void setMetaData(String group) {
        try {
            JSONParser jsonParser = new JSONParser();
            FileReader file1 = new FileReader(dbPath + group + "\\" + this.dbName + "\\"
                    + "metadata" + ".json");
            Object obj = jsonParser.parse(file1);
            this.metaData = (JSONObject) obj;
        } catch (Exception e) {
            System.out.println("No metadata found for " + this.dbName);
        }
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
}
