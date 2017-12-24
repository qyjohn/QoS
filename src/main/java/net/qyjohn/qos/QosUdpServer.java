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


public class QosUdpServer
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

	public QosUdpServer(int port)
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
			// Create a set of server threads
			new QosUdpServerThread(credits, defaults, jdbc, port).start();
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
		int port  = 51122;
		try
		{
			port  = Integer.parseInt(args[0]);
		} catch (Exception e){}
		new QosUdpServer(port);
	}
}





class QosUdpServerThread extends Thread
{
	ConcurrentHashMap<String, CreditRule> credits;
	DefaultSetting defaults;
	QosJdbcDriver jdbc;
	String hostIp;
	DatagramSocket socket;

	public QosUdpServerThread(ConcurrentHashMap<String, CreditRule> credits, DefaultSetting defaults, QosJdbcDriver jdbc, int port)
	{
		try
		{
			this.credits  = credits;
			this.defaults = defaults;
			this.jdbc = jdbc;		
			hostIp = InetAddress.getLocalHost().getHostAddress();
			socket = new DatagramSocket(port);
		} catch(Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void run()
	{
		String request, response, sql;
		CreditRule rule;
		int credit, rate, bound, port;
		Connection conn;
		Statement statement;
		ResultSet resultSet;

		while (true) 
		{
			try 
			{
				// receive request
				byte[] buf = new byte[4];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				request = new String(packet.getData(), 0, packet.getLength());
				
				// QoS check
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
				response = "" + credit;
				buf = response.getBytes();
 
				// send the response to the client at "address" and "port"
				InetAddress address = packet.getAddress();
				port = packet.getPort();
				packet = new DatagramPacket(buf, buf.length, address, port);
				socket.send(packet);
            		} catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
}
