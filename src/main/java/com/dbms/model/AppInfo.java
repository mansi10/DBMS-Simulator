package com.dbms.model;

import java.util.ArrayList;
import java.util.List;

public class AppInfo {

    private static AppInfo appInfo;
    private List<User> userList;
    private List<String> userGroup;

    private AppInfo(){}

    public static AppInfo getInstance(){
        if(null == appInfo){
            appInfo = new AppInfo();
            appInfo.setUserGroup(new ArrayList<>());
            appInfo.getUserGroup().add("Dalhousie");
            appInfo.getUserGroup().add("St Marys");
            appInfo.setUserList(new ArrayList<>());
        }
        return appInfo;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    public List<String> getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(List<String> userGroup) {
        this.userGroup = userGroup;
    }
}
