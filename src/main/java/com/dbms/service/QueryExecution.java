package com.dbms.service;

import com.dbms.common.Validation;
import com.dbms.model.CompleteDatabase;
import com.dbms.model.Query;
import com.dbms.model.User;
import com.dbms.presentation.ConsoleOutput;
import com.dbms.presentation.ReadUserInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class QueryExecution {

    private List<String> ddlList;
    private List<String> dmlList;
    private String showTables = "show tables";
    private String showDatabases = "show databases";
    private String desc = "desc";
    private String use = "use";
    private String generateERD = "generateERD";
    private String create = "create";
    private String exportDump = "export";

    @Autowired
    private DDLController ddlController;

    @Autowired
    private DMLController dmlController;

    @Autowired
    private GeneralController generalController;

    @Autowired
    private Validation validation;

    @Autowired
    private ERDGenerator erdGenerator;

    @Autowired
    private ExportDump dumpExporter;

    public QueryExecution(){
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

    public Query queryConsole(User user, Query query) throws Exception {
        String userResponse = query.getUserQuery();
        if (validation.isValidInput(userResponse)) {
            List<String> output = new ArrayList<>();
            userResponse = userResponse.trim();
            int userResponseLength = userResponse.length();
            if (userResponseLength > 3) {
                if (userResponse.substring(userResponseLength - 2, userResponseLength - 1)
                        .equalsIgnoreCase(";")) {
                    userResponse = userResponse.substring(0, userResponseLength - 2);
                }
                String[] words = userResponse.split(" ");
                System.out.println("words=" + words);
                String queryType = words[0];
                System.out.println("word0=" + words[0]);

                if (userResponse.equalsIgnoreCase(showDatabases)) {
                    output = generalController.showDatabases(user);
                } else if (queryType.equalsIgnoreCase(use)) {
                    user = generalController.loadDB(user, userResponse);
                    if (user.getCompleteDatabase() != null) {
                        output.add("Database loaded");
                    } else {
                        query.setError(true);
                        output.add("Database is not loaded. Please try again.");
                    }
                } else {
                    CompleteDatabase completeDatabase = user.getCompleteDatabase();

                    if (completeDatabase == null && !create.equalsIgnoreCase(queryType)) {
                        query.setError(true);
                        output.add("Please load a database to show the tables in the database.");
                    } else if (userResponse.equalsIgnoreCase(showTables)) {
                        output = generalController.showTables(user);
                    } else if (queryType.equalsIgnoreCase(desc)) {
                        output = generalController.descTable(user, userResponse);
                    } else if (ddlList.contains(queryType)) {
                        output.add(ddlController.processQuery(user, userResponse));
                    } else if (dmlList.contains(queryType)) {
                        output.addAll(dmlController.processQuery(user, userResponse));
                    } else if (userResponse.equalsIgnoreCase(generateERD)) {
                        output.add(erdGenerator.generateERD(user.getUserGroup(), user.getCompleteDatabase().getDbName()));
                    } else if (queryType.equalsIgnoreCase(exportDump)) {
                        output = dumpExporter.exportSQLDump(user, userResponse);
                    } else {
                        query.setError(true);
                        output.add("Invalid query.");
                    }
                }
                if (!query.isError()) {
                    query.setResultFlag(true);
                    if (query.getResultList() == null) {
                        query.setResultList(new ArrayList<>());
                    }
                    query.getResultList().addAll(output);
                } else {
                    query.setError(true);
                    query.setAppResponse(output.get(0));
                }
            }
        }
        return query;
    }


}
