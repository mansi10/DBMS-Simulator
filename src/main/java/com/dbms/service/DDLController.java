package com.dbms.service;

import com.dbms.model.User;
import com.dbms.service.parser.CreateDB;
import com.dbms.service.parser.CreateTable;
import com.dbms.service.parser.DropQuery;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DDLController {

    private final String create = "create";
    private final String drop = "drop";
    private final String truncate = "truncate";
    private final String alter = "alter";

    @Autowired
    private CreateDB createdbQuery;
    
    @Autowired
    private CreateTable createtableQuery;

    @Autowired
    private DropQuery dropQuery;

    public String processQuery(User user, String query) throws IOException {
        String words[] = query.split(" ");
        int queryWordCount = words.length;
        if(queryWordCount > 1) {
            String qyeryType = words[0];
            switch (qyeryType) {
                case create:
                	if(query.contains("DATABASE")||(query.contains("database"))) {
                	    return createdbQuery.runDBQuery(query, user);
                	}
                	if(query.contains("TABLE")||(query.contains("table"))) {
                		return createtableQuery.runTableQuery(query, user);
                	}
                    break;
                case alter:

                    break;
                case drop:
                    return dropQuery.runQuery(query, user);
                case truncate:

                    break;
            }
        }
        return null;
    }

    public JSONObject processQueryForTransaction(String query){
        String words[] = query.split(" ");
        int queryWordCount = words.length;
        if(queryWordCount > 1) {
            String qyeryType = words[0];
            switch (qyeryType) {
                case create:
                    if(query.contains("DATABASE")||(query.contains("database"))) {
                        return createdbQuery.parseCreateDBQuery(query);
                    }
                    if(query.contains("TABLE")||(query.contains("table"))) {
                        return createtableQuery.parseCreateTableQuery(query);
                    }
                    break;
                case alter:

                    break;
                case drop:

                    break;
                case truncate:

                    break;
            }
        }
        return null;
    }


}
