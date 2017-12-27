package net.qyjohn.qos;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;


public class QosTest extends Thread
{
	public int id = 0;
	public String host = "localhost";
	public int count = 1000;
	public LinkedList<String> keys;
	int[] latencies;
	public QosJdbcDriver jdbc;
	
	public QosTest(int id, QosJdbcDriver jdbc, String host, int count)
	{
		this.id = id;
		this.host = host;
		this.jdbc = jdbc;
		this.count = count;
		keys = new LinkedList<String>();
		latencies = new int[count];

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
			String key, url;
			while (resultSet.next())
			{
				key = resultSet.getString("name");
				url = "http://" + host + "/qos/qos_tcp.php?key=" + key;
				keys.add(url);
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
		long time1 = System.currentTimeMillis();
		try
		{
			String key;
			int latency;
			for (int i=0; i<count; i++)
			{
				key = keys.get(i);
				latency = qosTest(key);
				latencies[i] = latency;
			}
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		Arrays.sort(latencies);
		System.out.println("P90: \t" + latencies[(int) (count * 0.90)]);
		System.out.println("P99: \t" + latencies[(int) (count * 0.99)]);
		System.out.println("P999: \t" + latencies[(int) (count * 0.999)]);
		System.out.println("P9999: \t" + latencies[(int) (count * 0.9999)]);
		long time2 = System.currentTimeMillis();
		int time = (int) (time2 - time1);
	}
	
	public int qosTest(String url)
	{
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		long time1 = System.nanoTime();
		try
		{
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			InputStream in = conn.getInputStream();
			while ((bytesRead = in.read(buffer)) >= 0)
			{
				// Simple throw away the output
			}
			in.close();
//			conn.disconnect();
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
		long time2 = System.nanoTime();
		int time = (int) (time2 - time1);
		return time;
	}
	
	public static void main(String[] args)
	{
		try
		{
			String host = args[0];
			int nProc = Integer.parseInt(args[1]);
			
			QosJdbcDriver jdbc = new QosJdbcDriver();
			QosTest workers[] = new QosTest[nProc];
			
			long time1, time2;
			int  time, rps;
			int total = 384000;
			int count = total / nProc;
			for (int i=0; i<nProc; i++)
			{
				workers[i] = new QosTest(i, jdbc, host, count);
			}
			for (int i=0; i<nProc; i++)
			{
				workers[i].start();
				workers[i].join();
			}
		} catch (Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}