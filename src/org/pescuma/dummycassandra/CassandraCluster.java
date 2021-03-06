package org.pescuma.dummycassandra;

import java.util.HashMap;
import java.util.Map;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;

public class CassandraCluster
{
	private final String name;
	private final String host;
	private final int port;
	private final Map<String, CassandraKeyspace> keyspaces = new HashMap<String, CassandraKeyspace>();
	
	private Cluster cluster;
	
	public CassandraCluster(String name, String host, int port)
	{
		this.name = name;
		this.host = host;
		this.port = port;
	}
	
	public CassandraCluster(String name, String host)
	{
		this(name, host, 9160);
	}
	
	public CassandraKeyspace addKeyspace(String name)
	{
		if (keyspaces.get(name) != null)
			throw new IllegalArgumentException("Keyspace already registered: " + name);
		
		CassandraKeyspace keyspace = new CassandraKeyspace(name);
		keyspaces.put(name, keyspace);
		return keyspace;
	}
	
	public void removeKeyspace(String name)
	{
		if (!isConnected())
			throw new IllegalStateException("You have to be connected to be able to remove a keyspace");
		
		CassandraKeyspace keyspace = keyspaces.get(name);
		if (keyspace == null)
			throw new IllegalArgumentException("Keyspace not registered: " + name);
		
		keyspaces.remove(name);
		
		try
		{
			keyspace.shutdown();
			
			cluster.dropKeyspace(name);
		}
		catch (HectorException e)
		{
			throw new CassandraException("Error removing keyspace", e);
		}
	}
	
	public void connect()
	{
		try
		{
			cluster = HFactory.getOrCreateCluster(name, host + ":" + port);
			
			for (CassandraKeyspace keyspace : keyspaces.values())
				keyspace.connect(cluster);
		}
		catch (HectorException e)
		{
			throw new CassandraException("Error connecting to cassandra", e);
		}
	}
	
	public void disconnect()
	{
		if (!isConnected())
			throw new IllegalStateException("You have to be connected to be able to disconnect");
		
		try
		{
			for (CassandraKeyspace keyspace : keyspaces.values())
				keyspace.shutdown();
			
			HFactory.shutdownCluster(cluster);
		}
		catch (HectorException e)
		{
			throw new CassandraException("Error shutting down cassandra connection", e);
		}
		finally
		{
			cluster = null;
		}
	}
	
	public boolean isConnected()
	{
		return cluster != null;
	}
	
	public CassandraKeyspace getKeyspace(String keyspace)
	{
		return keyspaces.get(keyspace);
	}
	
	public Iterable<CassandraKeyspace> getKeyspaces()
	{
		return keyspaces.values();
	}
}
