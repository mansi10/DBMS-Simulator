package com.dbms.service.parser;

import com.dbms.datasource.Resource;
import com.dbms.datasource.WriteFile;
import com.dbms.model.CompleteDatabase;
import com.dbms.model.User;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class DropQuery {

    @Autowired
    private WriteFile writeFile;

    @Autowired
    private Resource resource;

    public String runQuery(String selectQuery, User user) throws IOException {
        String tableName = parseDropQuery(selectQuery);
        if(tableName == null){
            return "Query syntax is wrong. Kindly check and try again.";
        }
        if(dataDictionCheck(tableName, user)) {
            return executeDropQuery(tableName, user);
        } else{
            return "Table Name does not exist.";
        }
    }

    private boolean dataDictionCheck(String tableName, User user){
        boolean tableExists = false;
        CompleteDatabase completeDatabase = user.getCompleteDatabase();
        JSONObject metaData = completeDatabase.getMetaData();
        JSONArray tables = (JSONArray) metaData.get("tables");
        for(Object tableObj : tables){
            JSONObject tableJsonFul = (JSONObject) tableObj;
            for(Object keyObj : tableJsonFul.keySet()){
                String keyStr = (String) keyObj;
                if( tableName != null && keyStr.equalsIgnoreCase(tableName)){
                    tableExists = true;
                    break;
                }
            }
        }
        return tableExists;
    }

    public String parseDropQuery(String query){
        String words[] = query.split(" ");
        String output = null;
        if(words.length >2){
            String processedWords[] = new String[3];
            int i = 0;
            for(String word : words){
                if(!word.trim().isEmpty()) {
                    processedWords[i] = word;
                    i = i + 1;
                    if(i == 3){
                        break;
                    }
                }
            }
            if(processedWords.length > 2) {
                if (processedWords[0].equalsIgnoreCase("drop")
                        && processedWords[1].equalsIgnoreCase("table")) {
                    String tableName;
                    if (processedWords[2].contains(";")) {
                        tableName = processedWords[2].trim().substring(0, processedWords[2].length() - 2);
                    } else {
                        tableName = processedWords[2].trim();
                    }
                    output = tableName;
                }
            }
        }
        return output;
    }

    private String executeDropQuery(String tableName, User user) throws IOException {
        Object removeObj = null;
        CompleteDatabase completeDatabase = user.getCompleteDatabase();
        JSONObject metaData = completeDatabase.getMetaData();
        JSONArray tables = (JSONArray) metaData.get("tables");
        for(Object tableObj : tables){
            JSONObject tableJsonFul = (JSONObject) tableObj;
            for(Object keyObj : tableJsonFul.keySet()){
                String keyStr = (String) keyObj;
                if(keyStr.equalsIgnoreCase(tableName)){
                    removeObj = tableObj;
                    break;
                }
            }
        }

        if(removeObj != null) {
            tables.remove(removeObj);
            metaData.put("tables", tables);
            String path = user.getUserGroup() +"\\"+completeDatabase.getDbName()+"\\";
            writeFile.writeFile(metaData.toString(), path+"metadata.json");
            File file = new File(resource.dbPath+path+tableName+".json");
            if(file.delete()){
                return "Table dropped successfully.";
            }
        }
        return "Table drop failed.";
    }
}
