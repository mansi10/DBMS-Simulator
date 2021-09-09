package com.dbms.service;

import com.dbms.datasource.ReadFile;
import com.dbms.model.CompleteDatabase;
import com.dbms.model.User;
import com.dbms.presentation.ConsoleOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeneralController {

    @Autowired
    private ReadFile readFile;

    @Autowired
    private CreateLoadDatabase createLoadDatabase;

    public List<String> showTables(User user){
        List<String> output = new ArrayList<>();
        CompleteDatabase completeDatabase = user.getCompleteDatabase();
        Map<String, JSONArray> tableRecords = completeDatabase.getTableRecords();
        if(tableRecords == null || tableRecords.isEmpty()){
            output.add("There are no tables in the database.");
        } else{
            output.add("List of tables in "+user.getCompleteDatabase().getDbName()+" database are:");
            for(String tableName : tableRecords.keySet()){
                output.add("\t"+tableName+"\n");
            }
        }
        return output;
    }

    public List<String> showDatabases(User user) throws IOException {
        return readFile.readAllFolders(user.getUserGroup());
    }

    public List<String> descTable(User user, String query){
        List<String> output = new ArrayList<>();
        CompleteDatabase completeDatabase = user.getCompleteDatabase();
        JSONObject metaData = completeDatabase.getMetaData();
        if(metaData == null){
            output.add("There are no tables in the database.");
        } else{
            String words[] = query.split(" ");
            int queryWordCount = words.length;
            if(queryWordCount > 1) {
                String userTableName = words[1];
                JSONArray tables = (JSONArray) metaData.get("tables");
                for(Object table : tables){
                    JSONObject outerTableJson = (JSONObject) table;
                    String tableName=null;
                    for(Object tableNameObj : outerTableJson.keySet()){
                        tableName = (String) tableNameObj;
                    }
                    JSONObject innerTableJson = (JSONObject) outerTableJson.get(tableName);
                    if(tableName != null && tableName.equals(userTableName)){
                        JSONObject columns = (JSONObject) innerTableJson.get("columns");
                        output.add("------------------------------------------------------\n");
                        output.add("Column             |            Type      \n");
                        output.add("------------------------------------------------------\n");
                        for(Object columnMap : columns.keySet()){
                            String columnName = (String) columnMap;
                            String value = (String) columns.get(columnName);
                            output.add(columnName+"                 |               "+value+"\n");
                        }
                    }
                }
            } else{
                output.add("Invalid description query. Please try again.");
            }
        }
        return output;
    }

    public User loadDB(User user, String query) throws Exception {
        String words[] = query.split(" ");
        int queryWordCount = words.length;
        if(queryWordCount > 1) {
            String userDBName = words[1];
            createLoadDatabase.loadDatabase(user, userDBName);
        }
        return user;
    }

}
