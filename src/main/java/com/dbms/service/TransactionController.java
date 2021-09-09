package com.dbms.service;

import com.dbms.common.Validation;
import com.dbms.model.AppInfo;
import com.dbms.model.CompleteDatabase;
import com.dbms.model.Transaction;
import com.dbms.model.User;
import com.dbms.presentation.ConsoleOutput;
import com.dbms.presentation.ReadUserInput;
import com.dbms.service.parser.InsertQuery;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransactionController {

    private String showTables = "show tables";
    private String desc = "desc";
    private String exit = "exit";
    private List<String> ddlList;
    private List<String> dmlList;
    private String commit = "commit";
    private String rollback = "rollback";

    @Autowired
    private ReadUserInput readUserInput;

    @Autowired
    private Validation validation;

    @Autowired
    private DDLController ddlController;

    @Autowired
    private DMLController dmlController;

    @Autowired
    private GeneralController generalController;

    @Autowired
    private ConsoleOutput consoleOutput;

    public TransactionController(){
        createDDLList();
        createDMLList();
    }

    public void createDDLList(){
        ddlList = new ArrayList<>();
        ddlList.add("create");
        ddlList.add("alter");
        ddlList.add("drop");
        ddlList.add("truncate");
    }

    public void createDMLList(){
        dmlList = new ArrayList<>();
        dmlList.add("insert");
        dmlList.add("select");
        dmlList.add("update");
        dmlList.add("delete");
    }

    public Map<String, Object> parseQueryForTransaction(User user, String userResponse){
        Map<String, Object> outputMap = new HashMap<>();
        if (validation.isValidInput(userResponse)) {
            userResponse = userResponse.trim();
            int userResponseLength = userResponse.length();
            if (userResponseLength > 3) {
                if (userResponse.substring(userResponseLength - 2, userResponseLength - 1)
                        .equalsIgnoreCase(";")) {
                    userResponse = userResponse.substring(0, userResponseLength - 2);
                }
                String[] words = userResponse.split(" ");
                String queryType = words[0];

                if (userResponse.equalsIgnoreCase(showTables)) {
                    outputMap.put("show", generalController.showTables(user));
                } else if (queryType.equalsIgnoreCase(desc)) {
                    outputMap.put("desc", generalController.descTable(user, userResponse));
                } else if (ddlList.contains(queryType)) {
                    outputMap.put("ddl", ddlController.processQueryForTransaction(userResponse));
                } else if (dmlList.contains(queryType)) {
                    outputMap.put("dml", dmlController.processQueryForTransaction(userResponse));
                } else if (userResponse.equalsIgnoreCase(exit)) {
                    outputMap.put("exit", true);
                } else if (userResponse.equalsIgnoreCase(commit)){
                    outputMap.put("commit", true);
                } else if(userResponse.equalsIgnoreCase(rollback)){
                    outputMap.put("rollback", true);
                } else {
                    outputMap.put("invalid", true);
                }
            }
        }
        return outputMap;
    }

    public List<String> commitTransaction(User user){
        List<String> queries = user.getTransaction().getQueryList();
        List<String> output = new ArrayList<>();
        for(String query : queries){
            output.addAll(dmlController.processQuery(user, query));
        }
        return output;
    }

    public synchronized boolean checkTransaction(JSONObject jsonObject, User user) {
        List<User> userList = AppInfo.getInstance().getUserList();

        if (userList != null && !userList.isEmpty()) {
            for (User transUser : userList) {
                if(user.getUserGroup().equalsIgnoreCase(transUser.getUserGroup())) {
                    if (transUser.getUserName().equalsIgnoreCase(user.getUserName())) {
                        continue;
                    }
                    if(user.getCompleteDatabase().getDbName()
                            .equalsIgnoreCase(transUser.getCompleteDatabase().getDbName())) {
                        Transaction transaction = transUser.getTransaction();
                        if (transaction != null) {
                            List<String> queriesStr = transaction.getQueryList();
                            if (queriesStr != null && !queriesStr.isEmpty()) {
                                for (String queryStr : queriesStr){
                                    JSONObject transQueryJson = dmlController.processQueryForTransaction(queryStr);
                                    String transTableName = (String) transQueryJson.get("tableName");
                                    String tableName = (String) jsonObject.get("tableName");
                                    if (tableName != null && transTableName != null
                                            && tableName.equalsIgnoreCase(transTableName)) {
                                        if (jsonObject.get("conditionCol") == null
                                                || transQueryJson.get("conditionCol") == null) {
                                            System.out.println("transaction conflict");
                                            return false;
                                        }
                                    }
                                    String transColumnName = (String) transQueryJson.get("conditionCol");
                                    String columnName = (String) jsonObject.get("conditionCol");
                                    String transColumnValue = (String) transQueryJson.get("conditionVal");
                                    String columnValue = (String) jsonObject.get("conditionVal");
                                    if (tableName.equalsIgnoreCase(transTableName)) {
                                        JSONObject metaDataFile = user.getCompleteDatabase().getMetaData();
                                        JSONArray tablesMetaData = (JSONArray) metaDataFile.get("tables");
                                        String primaryKey = null;
                                        for (Object curObj : tablesMetaData) {
                                            JSONObject tableObj = (JSONObject) curObj;
                                            JSONObject currentTableMetadata = (JSONObject) tableObj.get(tableName);
                                            if (currentTableMetadata != null) {
                                                primaryKey = (String) currentTableMetadata.get("primaryKey");
                                                break;
                                            }
                                        }
                                        if(columnName.equalsIgnoreCase(primaryKey)
                                                && columnName.equalsIgnoreCase(transColumnName)
                                                && columnValue.equals(transColumnValue)){

                                            System.out.println("transaction conflict");
                                            return false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

}
