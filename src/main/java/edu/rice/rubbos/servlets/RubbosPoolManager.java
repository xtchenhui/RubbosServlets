/**
 * RUBBoS: Rice University Bulletin Board System.
 * Copyright (C) 2001-2004 Rice University and French National Institute For
 * Research In Computer Science And Control (INRIA).
 * Contact: jmob@objectweb.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * Initial developer(s): Emmanuel Cecchet.
 * Contributor(s): Niraj Tolia.
 */

package edu.rice.rubbos.servlets;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Stack;
import java.util.Random;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;

import java.io.PrintStream;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Provides the method to initialize connection to the database. All the
 * servlets inherit from this class
 */
public class RubbosPoolManager implements RubbosPoolMBean {

	/** Controls connection pooling */
	private static final boolean enablePooling = true;

	private static boolean poolInitiMarker = false; // add by qingyang
	private static int poolSize = Config.BrowseCategoriesPoolSize;
	private static Random generator = new Random(java.lang.System.currentTimeMillis());

	private static ArrayList<DBHost> hosts = new ArrayList<DBHost>();
	private static HashMap<Connection, DBHost> connlookup = new HashMap<Connection, DBHost>();
	private static HashMap<String, DBHost> hostlookup = new HashMap<String, DBHost>();
	private static Object DBconnLOCK = new Object(); // locking object shared by
														// all servlets
	private static int count = 0;
	private static int activeMysql = 1;
	private static int activetcat = 1;
	private static Vector<InputStream> inStreams = new Vector<InputStream>();
	private static Vector<Properties> dbProperties = new Vector<Properties>();
	private static String user;
	private static String passwd;
	// only the first position in the vector is the master DB
	private static int readWriteIndex = 0;
	static PrintStream buddy = null;

	

	static {
		try {
			buddy = new PrintStream(new FileOutputStream("/tmp/connpool.log"));
		} catch (Exception e) {
		}
	}

	/** Load the driver and get a connection to the database */
	public RubbosPoolManager() {
	}

	public static void init() throws Exception {

		try {
			synchronized (DBconnLOCK) {
				if (poolInitiMarker == true) {
					return;
				} else {
					poolInitiMarker = true;
				}

				for (int i = 0; i < Config.DatabasePropertiesSize; i++) {
					dbProperties.addElement(new Properties());
				}

				InputStream tempStream = null;

				String tempStr = Config.DatabaseProperties[0];

				tempStream = new FileInputStream(tempStr);
				((Properties) dbProperties.elementAt(0)).load(tempStream);
				inStreams.addElement(tempStream);

				
				Class.forName(((Properties) dbProperties.elementAt(0)).getProperty("datasource.classname"));

				initializeConnections();
			}
		} catch (FileNotFoundException f) {
			throw new UnavailableException("Couldn't find file mysql.properties: " + f + "<br>");
		} catch (IOException io) {
			throw new UnavailableException("Cannot open read mysql.properties: " + io + "<br>");
		} catch (ClassNotFoundException c) {
			throw new UnavailableException("Couldn't load database driver: " + c + "<br>");
		} catch (SQLException s) {
			throw new UnavailableException("Couldn't get database connection: " + s + "<br>");
		} finally {
			try {
				InputStream in = null;
				for (int i = 0; i < Config.DatabasePropertiesSize; i++) {
					in = ((InputStream) inStreams.elementAt(i));
					if (in != null)
						in.close();

				}

			} catch (Exception e) {
			}
		}
	}

	/**
	 * Initialize the pool of connections to the database. The caller must
	 * ensure that the driver has already been loaded else an exception will be
	 * thrown.
	 *
	 * @exception SQLException
	 *                if an error occurs
	 */
	public static synchronized void initializeConnections() throws SQLException {

		// This implementation must be changed once the databases are
		// modularized away
		
		Properties tempProp = ((Properties) dbProperties.elementAt(0));
		user = tempProp.getProperty("datasource.username");
		passwd = tempProp.getProperty("datasource.password");
		String hostname = tempProp.getProperty("datasource.host");
		int port = Integer.valueOf(tempProp.getProperty("datasource.port"));
		DBHost firstDB = new DBHost(hostname,port,user,passwd,poolSize);
		hosts.add(firstDB);
		hostlookup.put(hostname, firstDB);
	}

	/**
	 * implement methods from RubbosMBean add by hchen
	 */
	public int getPool() {
		return poolSize;
	}

	public boolean setPool(int pool){
		if( poolSize == pool)
			return true;
		else{
			poolSize = pool;
			int newsize = poolSize/activetcat;
			for(DBHost host:hosts){
				int delta = host.getConnSize() - newsize;
				if(delta > 0){
					host.reduceConn(delta);
				}else{
					host.addConn(-1*delta);
				}
			}
		}
		
		return true;
	}
	@Override
	public boolean setTcat(int tcat, String Mysqls) {
		if( activetcat == tcat)
			return true;
		else{
			
			String[] exists = Mysqls.split(";");
			activeMysql = exists.length;
			if (tcat > activetcat){			
				activetcat = tcat;
				for(String url:exists){					
					String host = url.split(":")[0];
					int port = Integer.valueOf(url.split(":")[1]);
					if(!hostlookup.containsKey(host))
						addMysql(host,port);
					else{
						DBHost oldhost = hostlookup.get(host);
						int newsize = poolSize/activetcat;
						oldhost.reduceConn(oldhost.getConnSize()-newsize);
					}
				}
			}else{
				activetcat = tcat;
				int newsize = poolSize/activetcat;
				for(DBHost host:hosts){
					host.addConn(newsize-host.getConnSize());
				}
			}
		}
		
		return true;
	}

	@Override
	public boolean addMysql(String host, int port) {
		activeMysql++;
		int conns = poolSize/activetcat;
		DBHost newhost = new DBHost(host,port,user,passwd,conns);
		hosts.add(newhost);
		hostlookup.put(host, newhost);
		return true;
	}

	@Override
	public boolean rmMysql(String host) {
		DBHost targethost = hostlookup.get(host);
		targethost.finalizedConnections();
		hosts.remove(targethost);
		activeMysql--;
		return true;
	}
	


	/**
	 * Closes a <code>Connection</code>.
	 * 
	 * @param connection
	 *            to close
	 */
	public static void closeConnection(Connection connection) {
		try {
			connection.close();
		} catch (Exception e) {

		}
	}

	/**
	 * Gets a connection from the pool that can do reading and writing (random)
	 *
	 * @return a <code>Connection</code> or null if no connection is available
	 */
	public static Connection getRWConnection() {
		DBHost host = schedule();
		Connection conn = host.getConnection();
		if (!connlookup.containsKey(conn))
			connlookup.put(conn, host);
		return conn;
	}

	/**
	 * Gets a connection from the pool that can only be used for reading
	 * (round-robin) least connection
	 *
	 * @return a <code>Connection</code> or null if no connection is available
	 */
	
	public static DBHost schedule(){
		DBHost res = hosts.get(0);
		int max = res.getConnSize();
		for (DBHost host:hosts){
			if(host.getConnSize() > max){
				max = host.getConnSize();
				res = host;
			}
		}
		return res;		
	}
	
	public static Connection getRConnection(String service) {
		DBHost host = schedule();
		Connection conn = host.getConnection();
		if (!connlookup.containsKey(conn))
			connlookup.put(conn, host);
		return conn;
	}

	/**
	 * Releases a read connection to the pool.
	 *
	 * @param c
	 *            the connection to release
	 */
	public static void releaseRConnection(Connection c) {
		if (enablePooling) {
			DBHost host = connlookup.get(c);
			host.ReleaseConn(c);
		}
	}

	/**
	 * Releases a read/write connection to the pool.
	 *
	 * @param c
	 *            the connection to release
	 */
	public static synchronized void releaseRWConnection(Connection c) {
		if (enablePooling) {
			DBHost host = connlookup.get(c);
			host.ReleaseConn(c);
		}
	}

	/**
	 * Release all the connections to the database.
	 *
	 * @exception SQLException
	 *                if an error occurs
	 */
	public static synchronized void finalizeConnections() throws SQLException {
		if (enablePooling) {
			while(hosts.size() > 0){
				DBHost host = hosts.remove(0);
				host.finalizedConnections();
			}
		}
	}

	/**
	 * Clean up database connections.
	 */
	public static void destroy() {
		try {
			finalizeConnections();
		} catch (SQLException e) {
		}
	}


}
