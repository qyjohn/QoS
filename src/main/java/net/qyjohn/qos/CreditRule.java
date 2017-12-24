package net.qyjohn.qos;

import java.util.*;


public class CreditRule
{
	public long time = System.currentTimeMillis();
	public int rate, bound, credit;
	public boolean system = true;

	public CreditRule(int rate, int bound)
	{
		this.rate  = rate;
		this.bound = bound;
		credit = bound;
	}

	// system = false, represents a temporary rule not in DB
	public CreditRule(int rate, int bound, boolean system)
	{
		this.rate  = rate;
		this.bound = bound;
		credit = bound;
		this.system  = system;
	}
	
	/**
	 *
	 * Refills the credit bucket.
	 *
	 */

	public synchronized void refill()
	{
		credit = credit + rate;
		if (credit > bound)
		{
			credit = bound;
		}
	}
	
	/**
	 *
	 * Updates the rules with a new set of refill rate and upper bound.
	 *
	 */
	 
	public synchronized void update(int rate, int bound)
	{
		this.rate  = rate;
		this.bound = bound;	
	}

	/**
	 *
	 * If there is still credit to consume then return 1, otherwise return 0 to throttle.
	 *
	 */

	public synchronized int consume()
	{
		time = System.currentTimeMillis();
		if (credit > 0)
		{
			credit = credit - 1;
//			return credit;		// for debug
			return 1;
		}
		else
		{
			return 0;
		}
	}
}
