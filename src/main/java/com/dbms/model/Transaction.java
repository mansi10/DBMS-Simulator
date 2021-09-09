package com.dbms.model;

import org.json.simple.JSONObject;

import java.util.List;

public class Transaction {

    private List<String> queryList;

    public List<String> getQueryList() {
        return queryList;
    }

    public void setQueryList(List<String> queryList) {
        this.queryList = queryList;
    }

    private List<JSONObject> queryJsonList;

    public List<JSONObject> getQueryJsonList() {
        return queryJsonList;
    }

    public void setQueryJsonList(List<JSONObject> queryJsonList) {
        this.queryJsonList = queryJsonList;
    }
}
