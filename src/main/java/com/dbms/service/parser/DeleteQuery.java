package com.dbms.service.parser;

import com.dbms.model.User;
import com.dbms.presentation.IConsoleOutput;
import com.dbms.service.TransactionController;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeleteQuery {

    @Autowired
    private IConsoleOutput logger;

    @Autowired
    private Utils utils;

    @Autowired
    private TransactionController transactionController;

    private final String errorMessage = "Invalid delete query. Please check syntax/spacing.";
    private final String tableNameRegex = "(\\w+)";
    private final String ConditionValueTypes = "(\".*\"|\\d+(?:.\\d+)?|TRUE|true|FALSE|false)";
    private final String conditionEquality = "(=|<=|>=|>|<|!=)";
    private final String conditionRegex = "(?:(?:\\sWHERE\\s)(?:(\\w+)" + "\\s?" + (conditionEquality) + "\\s?" + ConditionValueTypes + "))?";
    private final String deleteRegex = "DELETE FROM\\s" +
            tableNameRegex +
            conditionRegex +
            ";?$";

    public List<String> runQuery(String deleteQuery, User user) {
        JSONObject parsedQuery = parseDeleteQuery(deleteQuery);
        if(transactionController.checkTransaction(parsedQuery, user)) {
            return executeDeleteQuery(parsedQuery, user);
        }
        return new ArrayList<>(Arrays.asList("Transaction error." +
                "\nThe record or records involved in this query been modified by another transaction." +
                "\nPlease try again. "));
    }

    public JSONObject parseDeleteQuery(String deleteQuery) {
        JSONObject selectObject = new JSONObject();
        try {
            Pattern syntaxExp = Pattern.compile(deleteRegex, Pattern.CASE_INSENSITIVE);
            Matcher queryParts = syntaxExp.matcher(deleteQuery);
            String tableName = null;
            String conditionCol = null;
            String conditionType = null;
            String conditionVal = null;
            if(queryParts.find()) {
                tableName = queryParts.group(1);
                conditionCol = queryParts.group(2);
                conditionType = queryParts.group(3);
                conditionVal = queryParts.group(4);
            } else {
                selectObject.put("error", errorMessage);
                return selectObject;
            }
            selectObject.put("tableName", tableName);
            selectObject.put("conditionCol", conditionCol);
            selectObject.put("conditionType", conditionType);
            selectObject.put("conditionVal", conditionVal);
            return selectObject;
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    public List<String> executeDeleteQuery(JSONObject parsedQuery, User user) {
        List<String> output = new ArrayList<>();
        try {
            String tableName = (String) parsedQuery.get("tableName");
            String conditionCol = (String) parsedQuery.get("conditionCol");
            String conditionType = (String) parsedQuery.get("conditionType");
            String conditionVal = (String) parsedQuery.get("conditionVal");
            if (tableName.isEmpty()) {
                logger.error(errorMessage);
                output.add(errorMessage);
                return output;
            }

            String dbName = user.getCompleteDatabase().getDbName();
            JSONObject metaData = user.getCompleteDatabase().getMetaData();
            Map<String, JSONArray> tableRecords = user.getCompleteDatabase().getTableRecords();
            JSONArray tablesMetaData = (JSONArray) metaData.get("tables");
            JSONObject currentTableMetadata = null;
            for (Object curObj : tablesMetaData) {
                JSONObject tableObj = (JSONObject) curObj;
                JSONObject metadata = (JSONObject) tableObj.get(tableName);
                if (metadata != null) {
                    currentTableMetadata = (JSONObject) metadata.get("columns");
                    break;
                }
            }
            if (currentTableMetadata == null) {
                logger.error("Unable to fetch table metadata");
                output.add("Unable to fetch table metadata");
                return output;
            }
            JSONArray currentTableRecords = tableRecords.get(tableName);

            Map<String, Object> map = deleteRows(conditionCol, conditionType, conditionVal,
                    currentTableMetadata, currentTableRecords);

            JSONArray updatedRecords = (JSONArray) map.get("rows");
            String fileNameWithDB = user.getUserGroup()+"\\"+dbName + "\\" + tableName + ".json";

            utils.updateTableFile(updatedRecords.toJSONString(), fileNameWithDB);
            output.add((String) map.get("count"));
            output.add("Deleted successfully");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            output.add(e.getMessage());
        }
        return output;
    }

    private Map<String, Object> deleteRows(String col, String condition, String value,
                                JSONObject metadata, JSONArray rows) {
        Map<String, Object> outputMap = new HashMap<>();
        List indexToDelete = new ArrayList();
        String colType = (String) metadata.get(col);

        if (col == null) {
            System.out.println(rows.size() + " rows deleted.");
            outputMap.put("count", rows.size());
            rows.clear();
            outputMap.put("rows", rows);
            return outputMap;
        }

        for (int i=0; i<rows.size(); i++) {
            boolean matched = false;
            JSONObject curObj = (JSONObject) rows.get(i);
            if (colType.contains("int")) {
                int originalValue = (int) (long) curObj.get(col);
                int givenValue = Integer.parseInt(value);
                switch(condition){
                    case "<":
                        matched = originalValue < givenValue;
                        break;
                    case "<=":
                        matched = originalValue <= givenValue;
                        break;
                    case ">":
                        matched = originalValue > givenValue;
                        break;
                    case ">=":
                        matched = originalValue >= givenValue;
                        break;
                    case "=":
                        matched = originalValue == givenValue;
                        break;
                    case "!=":
                        matched = originalValue != givenValue;
                        break;
                    default:
                        matched = false;
                        break;
                }
            } else if (colType.contains("varchar")) {
                String originalValue = (String) curObj.get(col);
                String givenValue = value;
                if (givenValue.startsWith("\"")) givenValue = givenValue.substring(1);
                if (givenValue.endsWith("\"")) givenValue = givenValue.substring(0, givenValue.length() - 1);
                switch(condition){
                    case "=":
                        matched = originalValue.equals(givenValue);
                        break;
                    case "!=":
                        matched = !originalValue.equals(givenValue);
                        break;
                    default:
                        matched = false;
                        break;
                }
            }
            if (matched) {
                indexToDelete.add(i);
            }
        }
        for(int i = 0; i< indexToDelete.size(); i++) {
            int index = (int) indexToDelete.get(i);
            rows.remove(index);
        }
        String printQuery = indexToDelete.size() + " row(s) deleted.";
        outputMap.put("count", indexToDelete.size());
        logger.info(printQuery);
        outputMap.put("rows", rows);
        return outputMap;
    }
}