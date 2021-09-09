package com.dbms.model;

import java.util.Date;

public class User implements Comparable<User>{

    private long id;
    private String userName;
    private String password;
    private Date createdDate;
    private Date lastLoggedInDate;
    private CompleteDatabase completeDatabase;
    private Transaction transaction;
    private String userGroup;
    private boolean isError;
    private String appResponse;
    private boolean transactionFlag;

    public boolean isTransactionFlag() {
        return transactionFlag;
    }

    public void setTransactionFlag(boolean transactionFlag) {
        this.transactionFlag = transactionFlag;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastLoggedInDate() {
        return lastLoggedInDate;
    }

    public void setLastLoggedInDate(Date lastLoggedInDate) {
        this.lastLoggedInDate = lastLoggedInDate;
    }

    public CompleteDatabase getCompleteDatabase() {
        return completeDatabase;
    }

    public void setCompleteDatabase(CompleteDatabase completeDatabase) {
        this.completeDatabase = completeDatabase;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public String getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(String userGroup) {
        this.userGroup = userGroup;
    }

    @Override
    public int compareTo(User user) {
        long compare = this.id - user.getId();
        if(compare > 0){
            return 1;
        } else if(compare < 0){
            return -1;
        }
        return 0;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        this.isError = error;
    }

    public String getAppResponse() {
        return appResponse;
    }

    public void setAppResponse(String appResponse) {
        this.appResponse = appResponse;
    }
}
