package com.dbms.datasource;

import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public final class Resource {
    public final String dbPath = Paths.get("").toAbsolutePath().toString() + "\\data\\";
    public final String dumpPath = Paths.get("").toAbsolutePath().toString() + "\\exports\\";
    public final String erdPath = Paths.get("").toAbsolutePath().toString() + "\\erd\\";
}
