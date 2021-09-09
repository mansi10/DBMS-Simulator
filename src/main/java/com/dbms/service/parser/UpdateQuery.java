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
public class UpdateQuery {

    @Autowired
    private IConsoleOutput logger;

    @Autowired
    private Utils utils;

    @Autowired
    private TransactionController transactionController;

    private final String errorMessage = "Invalid update query. Please check syntax/spacing.";
    private final String tableNameRegex = "(\\w+)";
    private final String valueTypes = "(?:\".*\"|\\d+(?:.\\d+)?|TRUE|true|FALSE|false)";
    private final String ConditionValueTypes = "(\".*\"|\\d+(?:.\\d+)?|TRUE|true|FALSE|false)";
    private final String conditionEquality = "(=|<=|>=|>|<|!=)";
    // no spaces allowed for updations
    private final String assignmentRegex = "((?:\\w+="+ valueTypes + ")(?:,\\w+="+ valueTypes + ")*)";
    private final String conditionRegex = "(?:(?:\\sWHERE\\s)(?:(\\w+)" + "\\s?" + (conditionEquality) + "\\s?" + ConditionValueTypes + "))?";
    private final String updateRegex = "UPDATE " +
            tableNameRegex +
            "\\sSET\\s" +
            assignmentRegex +
            conditionRegex +
            ";?$";

    public List<String> runQuery(String updateQuery, User user) {
        JSONObject parsedQuery = parseUpdateQuery(updateQuery);
        if(transactionController.checkTransaction(parsedQuery, user)) {
            return executeUpdateQuery(parsedQuery, user);
        }
        return new ArrayList<>(Arrays.asList("Transaction error." +
                "\nThe record or records involved in this query been modified by another transaction." +
                "\nPlease try again. "));
    }

    public JSONObject parseUpdateQuery(String updateQuery) {
        JSONObject insertObject = new JSONObject();
        try {
            Pattern syntaxExp = Pattern.compile(updateRegex, Pattern.CASE_INSENSITIVE);
            Matcher queryParts = syntaxExp.matcher(updateQuery);
            String tableName = null;
            String assignments = null;
            String conditionCol = null;
            String conditionType = null;
            String conditionVal = null;
            if(queryParts.find()) {
                tableName = queryParts.group(1);
                assignments = queryParts.group(2);
                conditionCol = queryParts.group(3);
                conditionType = queryParts.group(4);
                conditionVal = queryParts.group(5);
            } else {
                insertObject.put("error", errorMessage);
                return insertObject;
            }
            insertObject.put("tableName", tableName);
            insertObject.put("assignments", getMappings(assignments, "="));
            insertObject.put("conditionCol", conditionCol);
            insertObject.put("conditionType", conditionType);
            insertObject.put("conditionVal", conditionVal);
            return insertObject;
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    public List<String> executeUpdateQuery(JSONObject parsedQuery, User user) {
        List<String> output = new ArrayList<>();
        try {
            String tableName = (String) parsedQuery.get("tableName");
            JSONObject assignments = (JSONObject) parsedQuery.get("assignments");
            String conditionCol = (String) parsedQuery.get("conditionCol");
            String conditionType = (String) parsedQuery.get("conditionType");
            String conditionVal = (String) parsedQuery.get("conditionVal");
            if (tableName.isEmpty() || assignments.isEmpty()) {
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

            Map<String, Object> map = updateRows(conditionCol, conditionType, conditionVal,
                    currentTableMetadata, currentTableRecords, assignments);

            JSONArray updatedRows = (JSONArray) map.get("rows");

            String fileNameWithDB = user.getUserGroup() + "\\"+ dbName + "\\" + tableName + ".json";

            utils.updateTableFile(updatedRows.toJSONString(), fileNameWithDB);
            output.add(String.valueOf(map.get("count")) + " rows Updated successfully.");
            output.add("");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            output.add(e.getMessage());
        }
        return output;
    }

    private JSONObject getMappings(String assignments, String equality) {
        try {
            JSONObject assignmentMapping = new JSONObject();
            if(assignments == null || assignments.isEmpty()) {
                return assignmentMapping;
            }
            int currentIndex = 0;
            while(currentIndex < assignments.length()) {
                String key, val;
                if (assignments.charAt(currentIndex) == ',') {
                    currentIndex = currentIndex + 1;
                    continue;
                }
                int keyIndex = assignments.indexOf(equality, currentIndex);
                key = assignments.substring(currentIndex, keyIndex);
                int valStart = keyIndex + 1;
                int valEnd;
                if (assignments.charAt(valStart) == '"') {
                    valStart = valStart + 1;
                    valEnd = assignments.indexOf('"', valStart);
                } else {
                    valEnd = valStart + 1;
                    while(valEnd < assignments.length() && assignments.charAt(valEnd) != ',') {
                        valEnd = valEnd + 1;
                    }
                }
                val = assignments.substring(valStart, valEnd);
                assignmentMapping.put(key, val);
                currentIndex = valEnd + 1;
            }
            return assignmentMapping;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    private Map<String, Object> updateRows(String col, String condition, String value,
                                 JSONObject metadata, JSONArray rows, JSONObject assignments) {

        Map<String, Object> outputMap = new HashMap<>();
        JSONArray newRows = new JSONArray();
        int counter = 0;
        ArrayList<String> updateCols = new ArrayList<>(assignments.keySet());
        String colType = (String) metadata.get(col);
        for (int i = 0; i < rows.size(); i++) {
            boolean matched = false;
            JSONObject curObj = (JSONObject) rows.get(i);
            if (col == null) {
                matched = true;
            } else {
                if (colType.contains("int")) {
                    int originalValue = (int) (long) curObj.get(col);
                    int givenValue = Integer.parseInt(value);
                    switch (condition) {
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
                    switch (condition) {
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
            }
            if (matched) {
                counter++;
                for (int j = 0; j < updateCols.size(); j++) {
                    String currCol = updateCols.get(j);
                    String currentColType = (String) metadata.get(currCol);
                    if (currentColType.contains("int")) {
                        int value1 = Integer.parseInt((String) assignments.get(currCol));
                        curObj.put(currCol, value1);
                    } else if (currentColType.contains("varchar")) {
                        String value1 = (String) assignments.get(currCol);
                        if (value1.startsWith("\"")) value1 = value1.substring(1);
                        if (value1.endsWith("\"")) value1 = value1.substring(0, value1.length() - 1);
                        curObj.put(currCol, value1);
                    } else if (currentColType.contains("float")) {
                        float value1 = Float.parseFloat((String) assignments.get(currCol));
                        curObj.put(currCol, value1);
                    } else if (currentColType.contains("boolean")) {
                        boolean value1 = Boolean.parseBoolean((String) assignments.get(currCol));
                        curObj.put(currCol, value1);
                    }
                }
                newRows.add(curObj);
            } else {
                newRows.add(curObj);
            }
        }
        String printQuery = counter + " row(s) updated.";
        outputMap.put("count", counter);
        logger.info(printQuery);
        outputMap.put("rows", newRows);
        return outputMap;
    }
}