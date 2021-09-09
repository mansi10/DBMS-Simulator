package com.dbms.service.parser;

import com.dbms.model.User;
import com.dbms.presentation.IConsoleOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SelectQuery {

    @Autowired
    private IConsoleOutput logger;

    private static String dbPath = Paths.get("").toAbsolutePath().toString() + "\\data\\";

    private final String errorMessage = "Invalid select query. Please check syntax/spacing.";
    private final String tableNameRegex = "(\\w+)";
    private final String columnNameRegex = "((?:\\*)|(?:(?:\\w+)(?:,\\s?\\w+)*))";
    private final String ConditionValueTypes = "(\".*\"|\\d+(?:.\\d+)?|TRUE|true|FALSE|false)";
    private final String conditionEquality = "(=|<=|>=|>|<|!=)";
    private final String conditionRegex = "(?:(?:\\sWHERE\\s)(?:(\\w+)" + "\\s?" + (conditionEquality) + "\\s?" + ConditionValueTypes + "))?";
    private final String selectRegex = "SELECT\\s" +
            columnNameRegex +
            "\\sFROM\\s" +
            tableNameRegex +
            conditionRegex +
            ";?$";

    public List<String> runQuery(String selectQuery, User user) {
        JSONObject parsedQuery = parseSelectQuery(selectQuery);
        return executeSelectQuery(parsedQuery, user);
    }

    public JSONObject parseSelectQuery(String selectQuery) {
        JSONObject selectObject = new JSONObject();
        try {
            Pattern syntaxExp = Pattern.compile(selectRegex, Pattern.CASE_INSENSITIVE);
            Matcher queryParts = syntaxExp.matcher(selectQuery);
            String tableName = null;
            String columnNames = null;
            String conditionCol = null;
            String conditionType = null;
            String conditionVal = null;
            if(queryParts.find()) {
                columnNames = queryParts.group(1);
                tableName = queryParts.group(2);
                conditionCol = queryParts.group(3);
                conditionType = queryParts.group(4);
                conditionVal = queryParts.group(5);
            } else {
                selectObject.put("error", errorMessage);
                return selectObject;
            }
            selectObject.put("tableName", tableName);
            selectObject.put("columns", getColumnArray(columnNames));
            selectObject.put("conditionCol", conditionCol);
            selectObject.put("conditionType", conditionType);
            selectObject.put("conditionVal", conditionVal);
            return selectObject;
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage());
            return null;
        }
    }

    public List<String> executeSelectQuery(JSONObject parsedQuery, User user) {
        List<String> output = new ArrayList<>();
        try {
            String tableName = (String) parsedQuery.get("tableName");
            JSONArray columns = (JSONArray) parsedQuery.get("columns");
            String conditionCol = (String) parsedQuery.get("conditionCol");
            String conditionType = (String) parsedQuery.get("conditionType");
            String conditionVal = (String) parsedQuery.get("conditionVal");
            if (tableName.isEmpty() || columns.isEmpty()) {
                logger.error(errorMessage);
                output.add(errorMessage);
            }
            Map<String, JSONArray> tableRecords = user.getCompleteDatabase().getTableRecords();
            JSONObject metaData = user.getCompleteDatabase().getMetaData();
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
            }

            JSONArray currentTableRecords = tableRecords.get(tableName);
            List filteredRows;
            List filteredColumns = new ArrayList();

            if (conditionCol == null) {
                filteredRows = currentTableRecords;
            } else {
                filteredRows = filterRows(conditionCol, conditionType, conditionVal,
                        currentTableMetadata, currentTableRecords);
            }

            if (columns.size() == 1 && columns.get(0).equals("*")) {
                Set keys = currentTableMetadata.keySet();
                for(Object key : keys) {
                    key = (String) key;
                    filteredColumns.add(key);
                }
            } else {
                filteredColumns = (ArrayList) columns;
            }

            if (filteredColumns.size() < 1) {
                logger.error("Unable to fetch table columns");
                output.add("Unable to fetch table columns");
            }

            return displayOutput(filteredRows, filteredColumns);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            output.add(e.getMessage());
        }
        return output;
    }

    private JSONArray getColumnArray(String columnNames) {
        JSONArray columns = new JSONArray();
        if(columnNames == null || columnNames.isEmpty()) {
            return columns;
        }
        if (columnNames.equals("*")) {
            columns.add(columnNames);
        } else {
            columnNames = columnNames.substring(0, columnNames.length());
            String[] tempArray = columnNames.split(",");
            Collections.addAll(columns, tempArray);
        }
        return columns;
    }

    private List filterRows(String col, String condition, String value,
                                 JSONObject metadata, JSONArray rows) {

        List filteredRows = new ArrayList();
        String colType = (String) metadata.get(col);
        for (Object obj : rows) {
            boolean matched = false;
            JSONObject curObj = (JSONObject) obj;
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
                filteredRows.add(curObj);
            }
        }
        return filteredRows;
    }

    private List<String> displayOutput(List filteredRows, List filteredColumns) {
        List<String> output = new ArrayList<>();
        String separator = new String(new char[(45*filteredColumns.size()) + filteredColumns.size() +1])
                .replace('\0', '_');

        output.add(separator);
        String columns = "";
        for (Object filteredColumn : filteredColumns) {
            columns = "\t"+columns + ((String) filteredColumn) +"\t\t|\t\t";
        }
        output.add(columns);
        output.add(separator);

        for (Object filteredRow : filteredRows) {
            JSONObject row = (JSONObject) filteredRow;
            columns = "";
            for (Object filteredColumn : filteredColumns) {
                columns = "\t"+columns + row.get(filteredColumn) +"\t\t|\t\t";
            }
            output.add(columns);
            output.add(separator);
        }
        String printQuery = filteredRows.size() + " row(s) returned.";
        logger.info(printQuery);
        output.add(printQuery);
        return output;
    }
}
