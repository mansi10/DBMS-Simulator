package com.dbms.controller;

import com.dbms.model.AppInfo;
import com.dbms.model.Query;
import com.dbms.model.Transaction;
import com.dbms.model.User;
import com.dbms.presentation.IConsoleOutput;
import com.dbms.service.QueryExecution;
import com.dbms.service.TransactionController;
import com.dbms.service.UserAuthentication;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class WelcomeController {

    @Autowired
    private IConsoleOutput logger;

    @Autowired
    private UserAuthentication userAuthentication;

    @Autowired
    private QueryExecution queryExecution;

    @Autowired
    private TransactionController transactionController;

    private User loggedInUser;

    @GetMapping("/login")
    public String getLogin(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @GetMapping("/logout")
    public String getLogout(Model model){
        List<User> userList = AppInfo.getInstance().getUserList();
        loggedInUser = null;
        userList.remove(loggedInUser);
        return "index";
    }

    @PostMapping("/login")
    public String postLogin(@ModelAttribute User user, Model model) {
        try {
            loggedInUser = userAuthentication.loginUser(user);
            if(!loggedInUser.isError()) {
                List<User> userList = AppInfo.getInstance().getUserList();
                if (userList == null) {
                    userList = new ArrayList<>();
                    AppInfo.getInstance().setUserList(userList);
                }
                boolean addFlag = true;
                for(User fromList : userList){
                    if(fromList.getUserName().equalsIgnoreCase(user.getUserName())){
                        addFlag = false;
                        break;
                    }
                }
                if(addFlag) {
                    userList.add(loggedInUser);
                }
                model.addAttribute("user", loggedInUser);
                model.addAttribute("query", new Query());
                return "query";
            } else{
                model.addAttribute("user", loggedInUser);
                return "login";
            }
        } catch (Exception e){
            user.setError(true);
            user.setAppResponse("System faced unexpected exception. Please contact support.");
            return "login";
        }
    }

    @GetMapping("/query")
    public String getQuery(
            Model model) {
        Query query = new Query();
        if(loggedInUser != null) {
            query.setUserName(loggedInUser.getUserName());
        }
        model.addAttribute("query", query);
        return "query";
    }

    @GetMapping("/")
    public String welcomePageGet(Model model){
        return "index";
    }

    @GetMapping("/register")
    public String getRegister(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String postRegister(@ModelAttribute User user, Model model) {
        try {
            user = userAuthentication.registerUser(user);
            if(!user.isError()){
                model.addAttribute("user", user);
                return "login";
            } else{
                model.addAttribute("user", user);
                return "register";
            }
        } catch (Exception e){
            user.setError(true);
            user.setAppResponse("System faced unexpected exception. Please contact support.");
            return "register";
        }
    }

    @PostMapping("/query")
    public String postQuery(@ModelAttribute Query query, Model model) {
        try {
            logger.info("user query: "+query.getUserQuery());
            if(loggedInUser != null) {
                List<User> userList = AppInfo.getInstance().getUserList();
                for (User user : userList) {
                    if (user.getUserName().equalsIgnoreCase(loggedInUser.getUserName())) {
                        loggedInUser = user;
                        break;
                    }
                }
                query.setUserName(loggedInUser.getUserName());
                model.addAttribute("query", query);
                if (query.getUserQuery() != null && query.getUserQuery().contains("start transaction")) {
                    if (loggedInUser.getCompleteDatabase() != null) {
                        loggedInUser.setTransactionFlag(true);
                        query.setResultFlag(true);
                        query.setResultList(new ArrayList<>());
                        query.getResultList().add("Transaction started.");
                    } else {
                        query.setError(true);
                        query.setAppResponse("Load or create a database to use.");
                    }
                } else if (loggedInUser.isTransactionFlag()) {
                    query = transactionController(query);
                } else {
                    query = queryExecution.queryConsole(loggedInUser, query);
                }
                logger.info("Appresponse: " + query.getAppResponse());
                if (query.getResultList() != null) {
                    for (String result : query.getResultList()) {
                        logger.info("result: " + result);
                    }
                }
            } else{
                query.setError(true);
                query.setAppResponse("No logged in user.");
            }
        } catch (Exception e){
            e.printStackTrace();
            query.setError(true);
            query.setAppResponse("System faced unexpected exception. Please contact support.");
        }
        return "query";
    }

    public Query transactionController(Query query){
        Transaction transaction = loggedInUser.getTransaction();
        if(transaction == null){
            transaction = new Transaction();
            loggedInUser.setTransaction(transaction);
            loggedInUser.getTransaction().setQueryJsonList(new ArrayList<>());
            loggedInUser.getTransaction().setQueryList(new ArrayList<>());
        } else{
            if(loggedInUser.getTransaction().getQueryJsonList() == null){
                loggedInUser.getTransaction().setQueryJsonList(new ArrayList<>());
                loggedInUser.getTransaction().setQueryList(new ArrayList<>());
            }
        }
        Map<String, Object> outputMap = transactionController.parseQueryForTransaction(loggedInUser, query.getUserQuery());
        if(query.getResultList() == null){
            query.setResultList(new ArrayList<>());
        }
        query.getResultList().add("Query is added to the Transaction.");
        boolean commitFlag = false;
        if (outputMap.get("show") != null) {
            query.setResultFlag(true);
            query.getResultList().add((String) outputMap.get("show"));
        } else if (outputMap.get("desc") != null) {
            query.setResultFlag(true);
            query.getResultList().add((String) outputMap.get("show"));
        } else if (outputMap.get("ddl") != null) {
            JSONObject ddlObj = (JSONObject) outputMap.get("ddl");
            if(ddlObj.get("error") == null){
                loggedInUser.setTransactionFlag(false);
                commitFlag = true;
                transaction.getQueryList().add(query.getUserQuery());
            } else{
                query.setError(true);
                query.setAppResponse((String) ddlObj.get("error"));
            }
        } else if (outputMap.get("dml") != null) {
            JSONObject dmlObj = (JSONObject) outputMap.get("dml");
            if(dmlObj.get("error") == null){
                transaction.getQueryList().add(query.getUserQuery());
                query.setResultFlag(true);

            } else{
                query.setError(true);
                query.setAppResponse((String) dmlObj.get("error"));
            }
        } else if (outputMap.get("exit") != null) {
            loggedInUser.setTransactionFlag(false);
        } else if (outputMap.get("commit") != null){
            loggedInUser.setTransactionFlag(false);
            commitFlag = true;
        } else if (outputMap.get("rollback") != null){
            transaction.getQueryList().clear();
            transaction.getQueryJsonList().clear();
            query.setResultFlag(true);
            query.getResultList().add("Transaction rolledback successfully.");
            loggedInUser.setTransactionFlag(false);
        } else if (outputMap.get("invalid") != null){
            query.setError(true);
            query.setAppResponse("Invalid query. Please try again.");
        }
        if(commitFlag){
            query.setResultFlag(true);
            query.getResultList().addAll(transactionController.commitTransaction(loggedInUser));
            query.getResultList().add("Transaction committed successfully.");
            transaction.getQueryList().clear();
            transaction.getQueryJsonList().clear();
        }
        return query;
    }


}