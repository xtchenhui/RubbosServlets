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
import java.util.EmptyStackException;
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
public abstract class RubbosHttpServlet extends HttpServlet
{

  static PrintStream buddy = null;
  static{
      try{
          buddy =new PrintStream(new FileOutputStream("/tmp/dbreqs.log"));
      }catch(Exception e){}
  }

  /** Controls connection pooling */
    private static final boolean enablePooling = true;
  /** Stack of available connections (pool) */
  private Stack freeConnectionsRW = null;
  private Stack freeConnectionsROnly = null;


  private static boolean poolInitiMarker = false;  // add by qingyang

  private int poolSize;
  private Random generator = null;
  private Hashtable connectionLookup =null;
  private static Vector<Stack> databaseConnections = new Vector<Stack>(); //changed by qingyang by adding static
  private static Object DBconnLOCK = new Object(); //locking object shared by all servlets
  private Vector<InputStream> inStreams =null;
  private Vector<Properties> dbProperties = null;
  //only the first position in the vector is the master DB
  private static int readWriteIndex = 0;

  public abstract int getPoolSize(); // Get the pool size for this class

  /** Load the driver and get a connection to the database */
  public void init() throws ServletException
  {
	try{
	  RubbosPool.init();
  	}catch(Exception e){}
  }
 /**
  * Closes a <code>Connection</code>.
  * @param connection to close
  */
   private void closeConnection(Connection connection)
   {
      RubbosPool.closeConnection(connection);
   }



  /**
   * Gets a connection from the pool that can do reading and writing (random)
   *
   * @return a <code>Connection</code> or
   * null if no connection is available
   */
  public synchronized Connection getRWConnection()
  {
	return RubbosPool.getRWConnection();
  }



  /**
   * Gets a connection from the pool that can only be used for reading (round-robin)
   *
   * @return a <code>Connection</code> or
   * null if no connection is available
   */
  public  Connection getRConnection(String Service)
  {
   return RubbosPool.getRConnection(Service);
  }

  /**
   * Releases a read connection to the pool.
   *
   * @param c the connection to release
   */
  public  void releaseRConnection(Connection c )
  {
	RubbosPool.releaseRConnection(c);
  }


  /**
   * Releases a read/write connection to the pool.
   *
   * @param c the connection to release
   */
  public synchronized void releaseRWConnection(Connection c )
  {
	RubbosPool.releaseRWConnection(c);
  }

  /**
   * Release all the connections to the database.
   *
   * @exception SQLException if an error occurs
   */
  public synchronized void finalizeConnections() throws SQLException
  {
	RubbosPool.finalizeConnections();
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {

  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {

  }

  /**
   * Clean up database connections.
   */
  public void destroy()
  {
	RubbosPool.destroy();
  }

}
