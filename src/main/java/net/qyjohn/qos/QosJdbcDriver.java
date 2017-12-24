package net.qyjohn.qos;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class QosJdbcDriver
{
	public BoneCP connectionPool;
	
	public QosJdbcDriver()
	{
		try
		{
			// Getting database properties from qos.properties
			Properties prop = new Properties();
			InputStream input = new FileInputStream("qos.properties");
			prop.load(input);
			String db_hostname = prop.getProperty("db_hostname");
			String db_username = prop.getProperty("db_username");
			String db_password = prop.getProperty("db_password");
			String db_database = prop.getProperty("db_database");
			String jdbcUrl = "jdbc:mysql://" + db_hostname + "/" + db_database;
			
			// Creates the connection pool
			BoneCPConfig config = new BoneCPConfig();
			config.setJdbcUrl(jdbcUrl);
			config.setUsername(db_username);
			config.setPassword(db_password);
			connectionPool = new BoneCP(config); 
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();			
		}
	}

	/**
	 *
	 * Creates a JDBC connection to the back end database.
	 *
	 */
	 
	public Connection getJdbcConnection()
	{
		Connection conn = null;
		try
		{
			conn = connectionPool.getConnection();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();			
		}
		return conn;		
	}
}
