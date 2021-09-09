package com.dbms.service;

import com.dbms.common.Validation;
import com.dbms.datasource.ReadFile;
import com.dbms.datasource.Resource;
import com.dbms.model.CompleteDatabase;
import com.dbms.model.User;
import com.dbms.presentation.ConsoleOutput;
import com.dbms.presentation.ReadUserInput;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
public class CreateLoadDatabase {

    @Autowired
    private ReadUserInput readUserInput;

    @Autowired
    private Validation validation;

    @Autowired
    private ConsoleOutput consoleOutput;

    @Autowired
    private ReadFile readFile;

    @Autowired
    private Resource resource;

   public static String dbname;
   String datapath;

    private boolean checkDBNameUserName(String dbName, String userGroup){
        boolean isExist = false;
        File file = new File(resource.dbPath + userGroup + "\\" + dbName);
        if(file.exists() && file.isDirectory()){
            isExist = true;
            datapath=resource.dbPath;
        }
        return isExist;
    }

    public String loadDatabase(User user, String dbName) throws Exception {
        Map<String, JSONArray> tableRecords;
        dbname=dbName;
        if (checkDBNameUserName(dbName, user.getUserGroup())) {
            tableRecords = loadTables(dbName, user.getUserGroup());
            user.setCompleteDatabase(new CompleteDatabase());
            user.getCompleteDatabase().setTableRecords(tableRecords);
            user.getCompleteDatabase().setDbName(dbName);
            user.getCompleteDatabase().setMetaData(user.getUserGroup());
            return "Database is loaded successfully.";
        } else{
            return ("Database does not exist or User might not have permission for the Database.\n" +
                    "Please load existing database or create new.");
        }
    }

    private Map<String, JSONArray> loadTables(String dbName, String userGroup) throws Exception {
        return readFile.readFilesFromPath(dbName, userGroup);
    }


}
