package com.dbms.service.parser;

import com.dbms.model.CompleteDatabase;
import com.dbms.model.User;
import com.dbms.presentation.ConsoleOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CreateDB {

	public String dbPath = Paths.get("").toAbsolutePath().toString() + "\\data\\";
	private final String errorMessage = "Invalid create query. Please check syntax/spacing.";
	private final String databaseNameRegex = "(\\w+)";

	private  final String createRegex = "CREATE DATABASE\\s" +
			databaseNameRegex +
			";?$";
	JSONArray tables = new JSONArray();
	JSONObject object = new JSONObject();

	public String runDBQuery(String createdbQuery, User user) {
		JSONObject parsedQuery = parseCreateDBQuery(createdbQuery);
		return executeCreateDBQuery(parsedQuery, user);
	}

	public JSONObject parseCreateDBQuery(String createdbQuery) {
		JSONObject selectObject = new JSONObject();
		try {
			Pattern syntaxExp = Pattern.compile(createRegex, Pattern.CASE_INSENSITIVE);
			Matcher queryParts = syntaxExp.matcher(createdbQuery);
			String dbName = null;
			if(queryParts.find()) {
				dbName = queryParts.group(1);
			} else {
				selectObject.put("error", errorMessage);
				return selectObject;
			}
			selectObject.put("dbName", dbName);
			return selectObject;
		} catch(Exception e) {
			System.out.println(e.getLocalizedMessage());
			return null;
		}
	}

	public String executeCreateDBQuery(JSONObject parsedQuery, User user) {
		try {
			String dbName = (String) parsedQuery.get("dbName");
			if (dbName.isEmpty()) {
				System.out.println(errorMessage);
				return errorMessage;
			}
			if(createDirectory(dbName, user)) {

				try (FileWriter file =
							 new FileWriter(dbPath + user.getUserGroup() + "\\" +
									 dbName + "\\" + "metadata" + ".json")) {
					object.put("tables", tables);

					file.write(object.toJSONString());
					file.close();
					user.setCompleteDatabase(new CompleteDatabase());
					user.getCompleteDatabase().setDbName(dbName);

				} catch (IOException e) {
					e.printStackTrace();
					throw e;
				}
				return "Created Database successfully.";
			} else{
				return ("Database already exists. Kindly load the database to use.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	private boolean createDirectory(String dbName, User user) throws IOException {
		try {
			Path path = Paths.get(dbPath + user.getUserGroup()+"\\"+dbName);
			if(Files.exists(path)){
				return false;
			} else{
				Files.createDirectory(path);
			}
		} catch (IOException e){
			System.out.println(e.getLocalizedMessage());
			return false;
		}
		return true;
	}

}
