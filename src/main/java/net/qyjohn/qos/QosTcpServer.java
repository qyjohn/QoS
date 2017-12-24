package net.qyjohn.qos;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * QoS Server
 *
 */


public class QosTcpServer
{
	public QosJdbcDriver jdbc;
	public ConcurrentHashMap<String, CreditRule> credits;
	public DefaultSetting defaults;

	/**
	 *
	 * On initialization do the following:
	 * (1) Creates a HaspMap to store the quota and credit information;
	 * (2) Creates a JDBC connection to the back end database;
	 * (3) Starts a thread to refill the credits every 1000 ms.
	 * (4) Starts a server thread to accept incoming connections.
	 *
	 */

	public QosTcpServer(int port)
	{
		try
		{
			// Creates a JDBC connection to the database
			// Load the default rate, bound, and update interval
			jdbc = new QosJdbcDriver();
			defaults = new DefaultSetting();
			loadDefaultSettings();

			// Creates the credits HashMap and load the credit rules.
			credits = new ConcurrentHashMap<String, CreditRule>();
			// Creates the credit refill thread with the desired refill interval.
			new CreditRefillThread(credits, defaults.interval).start();
			
			// Creates a server socket and accepts incoming connections. 
			// Each incoming connection is handled by a separate thread.
			ServerSocket server = new ServerSocket(port);
			while (true)
			{
				new QosTcpServerThread(server.accept(), credits, defaults, jdbc).start();
			}
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();			
		}
	}
	
	
	/**
	 *
	 * Loads the default refill rate, upper bound, and update interval from DB.
	 *
	 */
	 
	public void loadDefaultSettings()
	{
		try
		{
			Connection conn = jdbc.getJdbcConnection();
			String sql = "SELECT * FROM defaults LIMIT 1";
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			if (resultSet.next())
			{
				defaults.rate     = resultSet.getInt("rate");
				defaults.bound    = resultSet.getInt("bound");
				defaults.interval = resultSet.getInt("duration");
			}
			conn.close();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();			
		}
	}
		
	public static void main(String[] args)
	{
		int port = 51122;
		try
		{
			port = Integer.parseInt(args[0]);
		} catch (Exception e){}
		new QosTcpServer(port);
	}
}


class QosTcpServerThread extends Thread
{
	Socket client;
	ConcurrentHashMap<String, CreditRule> credits;
	DefaultSetting defaults;
	QosJdbcDriver jdbc;
	String hostIp;

	public QosTcpServerThread(Socket client, ConcurrentHashMap<String, CreditRule> credits, DefaultSetting defaults, QosJdbcDriver jdbc)
	{
		try
		{
			this.client   = client;
			this.credits  = credits;
			this.defaults = defaults;
			this.jdbc = jdbc;		
			hostIp = InetAddress.getLocalHost().getHostAddress();
		} catch(Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void run()
	{
		try
		{
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			String request, sql;
			CreditRule rule;
			int credit, rate, bound;
			Connection conn;
			Statement statement;
			ResultSet resultSet;
			while ((request = in.readLine()) != null)
			{
				rule = credits.get(request);
				if (rule == null)
				{
					// The rule we are looking for is not in memory yet.
					// Check the DB for the rule
					conn = jdbc.getJdbcConnection();
					sql = "SELECT * FROM credits WHERE name='" + request + "' LIMIT 1";
					statement = conn.createStatement();
					resultSet = statement.executeQuery(sql);
					if (resultSet.next())
					{
						// There is a rule in the DB
						rate  = resultSet.getInt("rate");
						bound = resultSet.getInt("bound");
						rule = new CreditRule(rate, bound);
						credits.put(request, rule);
					}
					else
					{
						// Setup a temporary rule
						rule = new CreditRule(defaults.rate, defaults.bound, false);
						credits.put(request, rule);
					}
					conn.close();
				}
				credit = rule.consume();
				out.println(credit);
			}
			client.close();
		} catch(Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
