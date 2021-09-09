package com.dbms.service;

import com.dbms.model.User;
import com.dbms.service.parser.DeleteQuery;
import com.dbms.service.parser.InsertQuery;
import com.dbms.service.parser.SelectQuery;
import com.dbms.service.parser.UpdateQuery;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DMLController {

    private final String insert = "insert";
    private final String select = "select";
    private final String update = "update";
    private final String delete = "delete";

    @Autowired
    private InsertQuery insertQuery;

    @Autowired
    private DeleteQuery deleteQuery;

    @Autowired
    private SelectQuery selectQuery;

    @Autowired
    private UpdateQuery updateQuery;

    public DMLController(){
    }

    public List<String> processQuery(User user, String query){
        String words[] = query.split(" ");
        int queryWordCount = words.length;
        if(queryWordCount > 1) {
            String queryType = words[0];
            switch (queryType) {
                case insert:
                    return insertQuery.runQuery(query, user);
                case select:
                    return selectQuery.runQuery(query, user);
                case delete:
                    return deleteQuery.runQuery(query, user);
                case update:
                    return updateQuery.runQuery(query, user);
            }
        }
        return null;
    }

    public JSONObject processQueryForTransaction(String query){
        String words[] = query.split(" ");
        int queryWordCount = words.length;
        JSONObject jsonObject = null;
        if(queryWordCount > 1) {
            String queryType = words[0];
            switch (queryType) {
                case insert:
                    jsonObject = insertQuery.parseInsertQuery(query);
                    break;
                case select:
                    jsonObject = selectQuery.parseSelectQuery(query);
                    break;
                case delete:
                    jsonObject = deleteQuery.parseDeleteQuery(query);
                    break;
                case update:
                    jsonObject = updateQuery.parseUpdateQuery(query);
                    break;
            }
        }
        return jsonObject;
    }

}
