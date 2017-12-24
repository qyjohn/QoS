package net.qyjohn.qos;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CreditRefillThread extends Thread
{
	int interval;
	ConcurrentHashMap<String, CreditRule> credits;

	public CreditRefillThread(ConcurrentHashMap<String, CreditRule> credits, int interval)
	{
		this.credits = credits;
		this.interval = interval;
	}

	public void run()
	{
		while(true)
		{
			try
			{
				// Traverse through the HashMap
				Iterator it = credits.entrySet().iterator();
				while (it.hasNext()) 
				{
					Map.Entry entry = (Map.Entry)it.next();
					CreditRule rule = (CreditRule) entry.getValue();
					rule.refill();
				}
				sleep(interval);
			} catch(Exception e)
			{
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
