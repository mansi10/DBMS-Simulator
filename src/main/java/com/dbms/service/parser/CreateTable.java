package com.dbms.service.parser;

import com.dbms.datasource.IWriteFile;
import com.dbms.model.CompleteDatabase;
import com.dbms.model.User;
import com.dbms.presentation.ConsoleOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CreateTable {


	public String dbPath = Paths.get("").toAbsolutePath().toString() + "\\data\\";
	private final String errorMessage = "Invalid create table query. Please check syntax/spacing.";
	private final String tableNameRegex = "(\\w+)";
	private final String primaryKey = ", PRIMARY KEY[(](\\w+)[)]";
	private final String values = "[(]((((\\w+) (varchar|int)[(]\\d+[)])(,)*\\s*)+)"+(primaryKey)+"[)]";
	private final String createRegex = "CREATE TABLE\\s" +
			tableNameRegex + values+
			";?$";
	String tableName = null;
	JSONArray tables = new JSONArray();

	@Autowired
	private IWriteFile writeFile;

	@Autowired
	private ConsoleOutput consoleOutput;


	public String runTableQuery(String createdbQuery, User user) throws IOException {
		CompleteDatabase completeDatabase = user.getCompleteDatabase();
		if(completeDatabase == null){
			return ("Create or Load a database to create table.");
		}
		JSONObject parsedQuery = parseCreateTableQuery(createdbQuery);
		System.out.println("object received="+parsedQuery);
		if(parsedQuery != null && tableName != null && !tableName.isEmpty()) {
			boolean flag = addTable(user);
			if(flag){
				return executeCreateTableQuery(parsedQuery, user);
			}
		}
		return "Create table failed.";
	}

	public boolean addTable(User user) throws IOException {
		boolean addTable = false;
		CompleteDatabase completeDatabase = user.getCompleteDatabase();
		if(completeDatabase == null){
			completeDatabase = new CompleteDatabase();
			user.setCompleteDatabase(completeDatabase);
		}
		Map<String, JSONArray> tableRecords = completeDatabase.getTableRecords();
		if(tableRecords == null){
			tableRecords = new HashMap<>();
			completeDatabase.setTableRecords(tableRecords);
		}
		JSONArray records = tableRecords.get(tableName.toLowerCase());
		if(records == null){
			addTable = true;
			tableRecords.put(tableName.toLowerCase(), new JSONArray());
			writeFile.writeFile(new JSONArray().toJSONString(), user.getUserGroup()+"\\"+
					user.getCompleteDatabase().getDbName()+"\\"+tableName.toLowerCase()+".json");
		}
		return addTable;
	}

	public JSONObject parseCreateTableQuery(String createdbQuery) {
		JSONObject selectObject = new JSONObject();
		JSONArray tables = new JSONArray();

		try {
			Pattern syntaxExp = Pattern.compile(createRegex, Pattern.CASE_INSENSITIVE);
			Matcher queryParts = syntaxExp.matcher(createdbQuery);
			String attributes = null;
			String primary = null;

			if(queryParts.find()) {
				tableName = queryParts.group(1);

				attributes = queryParts.group(2);

				primary = queryParts.group(8);

			} else {
				selectObject.put("error", errorMessage);
				return selectObject;
			}
			JSONArray arr2 = new JSONArray();

			HashMap<String, String> map = new HashMap<String, String>();

			String s[]= attributes.split("[\\s,]+");
			JSONObject jsonobj = new JSONObject();

			for(int i=0;i<s.length-1;i=i+2) {

				JSONObject js = new JSONObject();
				map.put(s[i], s[i+1]);
				JSONObject jsonarr = new JSONObject();

				arr2.add(jsonarr);
				jsonarr.putAll(map);
				jsonobj=jsonarr;

			}
			JSONObject jsonobj2 = new JSONObject();
			jsonobj2.put("columns",jsonobj);
			jsonobj2.put("primaryKey",primary);
			JSONObject jsonobj3 = new JSONObject();
			jsonobj3.put(tableName, jsonobj2);  //json object for a single table created
			JSONObject jsonobj4 = new JSONObject();

			selectObject.put(tableName, jsonobj2);

			return selectObject;
		} catch(Exception e) {
			System.out.println(e.getLocalizedMessage());
			return null;
		}
	}

	public String executeCreateTableQuery(JSONObject parsedQuery, User user) {
		try {
			String table_Name = parsedQuery.get(tableName).toString();
			if (table_Name.isEmpty()) {
				System.out.println(errorMessage);
				return errorMessage;
			}
			System.out.println(parsedQuery);

			tables.add(parsedQuery);
			JSONParser jsonParser = new JSONParser();

			String dbName = user.getCompleteDatabase().getDbName();
			String userGroup = user.getUserGroup();

			System.out.println("dbname="+dbName);
			System.out.println("path="+(dbPath+dbName+"\\"+"metadata"+".json"));

			FileReader file1 = new FileReader(dbPath+userGroup+"\\"+dbName+"\\"+"metadata"+".json");
			Object obj = jsonParser.parse(file1);
			JSONObject object1 = (JSONObject) obj;
			JSONArray object2 = (JSONArray) object1.get("tables");
			object2.add(parsedQuery);

			for(int i=0;i<object2.size();i++) {

				System.out.println("object2="+object2);

			}	           
			try (FileWriter file = new FileWriter(dbPath+userGroup+"\\"+dbName+"\\"+"metadata"+".json", false)) {

				file.write("");

				JSONObject tableObject = new JSONObject();

				tableObject.put("tables", object2);
				System.out.println("tableObject="+tableObject); //json object for the whole metadata file

				file.write(tableObject.toString());
				System.out.println("Data Successfully added to database"); 

				file.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			user.getCompleteDatabase().setMetaData(user.getUserGroup());
			return "Table created.";
		} catch (Exception e) {
			System.out.println(e.getLocalizedMessage());
			return e.getMessage();
		}
	}


	/*    public static void main(String []a) {

		        String s3 = "create TABLE orders(productname varchar(60), id int(50), price int(50), PRIMARY KEY(id))";
		        String s1 = "create TABLE customers(name varchar(60), id int(50), age int(50), city varchar(50), PRIMARY KEY(id))";
		        String s2 = "create TABLE tablename(column varchar(60), ok int(50))";

		        runTableQuery(s1);
		    }*/
}


