package net.qyjohn.qos;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;


public class QosTest extends Thread
{
	public int id = 0;
	public String host = "localhost";
	public int port = 1234;
	public int mode = 0;
	public int count = 1000;
	public LinkedList<String> keys;
	public QosJdbcDriver jdbc;
	
	public QosTest(int id, QosJdbcDriver jdbc, String host, int port, int count)
	{
		this.id = id;
		this.host = host;
		this.port = port;
		this.jdbc = jdbc;
		this.count = count;
		keys = new LinkedList<String>();

		// Load Test Data
		try
		{
			Connection conn = jdbc.getJdbcConnection();
			Random random = new Random();
			int start = count * id;
			int stop  = start + count;
			String sql = String.format("SELECT name FROM credits LIMIT %d,%d", start, stop);
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next())
			{
				keys.add(resultSet.getString("name"));
			}
			conn.close();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();			
		}		
	}
	
	public void run()
	{
//		for (int repeat=0; repeat<2; repeat++)
//		{
			// This is a test with persistent connection
			// The first round of test reports gives first retrieval latency (QoS key not on QoS Server).
			// The other round of test reports gives regular retrieval latency (QoS key on Qos Server).
			long time1 = System.currentTimeMillis();
			try
			{
				Socket socket = new Socket(host, port);
				socket.setReuseAddress(true);
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				for (int i=0; i<count; i++)
				{
					out.println(keys.get(i));
					in.readLine();
				}
				socket.close();
			} catch (Exception e)
			{
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			long time2 = System.currentTimeMillis();
			int time = (int) (time2 - time1);
			float latency = (1000 * time) / count;
			System.out.println(latency);
//		}
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			String server = args[0];
			int port = Integer.parseInt(args[1]);
			int nProc = Integer.parseInt(args[2]);
			
			QosJdbcDriver jdbc = new QosJdbcDriver();
			QosTest workers[] = new QosTest[nProc];
			
			long time1, time2;
			int  time, rps;
			int total = 384000;
			int count = total / nProc;
			// First test, first request latency
			time1 = System.currentTimeMillis();
			for (int i=0; i<nProc; i++)
			{
				workers[i] = new QosTest(i, jdbc, server, port, count);
				workers[i].start();
			}
			for (int i=0; i<nProc; i++)
			{
				workers[i].join();
			}
			time2 = System.currentTimeMillis();
			time = (int) (time2 - time1);
			rps = total / (time / 1000);
			System.out.println("\nFirst request rps: " + rps + " in " + time + " milliseconds\n");


			// Second test, second request latency
			time1 = System.currentTimeMillis();
			for (int i=0; i<nProc; i++)
			{
				workers[i] = new QosTest(i, jdbc, server, port, count);
				workers[i].start();
			}
			for (int i=0; i<nProc; i++)
			{
				workers[i].join();
			}
			time2 = System.currentTimeMillis();
			time = (int) (time2 - time1);
			rps = total / (time / 1000);
			System.out.println("\nFirst request rps: " + rps + " in " + time + " milliseconds\n");
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}