package com.dbms.datasource;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Map;

public interface IReadFile {

    JSONArray readJSONArrayFromFile(String filePath) throws IOException, ParseException;

    Map<String, JSONArray> readFilesFromPath(String dirPath, String userGroup) throws Exception;
}
