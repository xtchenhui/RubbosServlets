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

/**
 * Provides the method to initialize connection to the database. All the
 * servlets inherit from this class
 */
public abstract class RubbosHttpServletBackup extends HttpServlet
{

  static PrintStream buddy = null;
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
      buddy = new PrintStream(new FileOutputStream("/tmp/buddysays"));
    } catch (Exception e){}


    connectionLookup=new Hashtable();

    generator = new Random( java.lang.System.currentTimeMillis() );
    poolSize = getPoolSize();
   // databaseConnections = new Vector<Stack>();
    inStreams = new Vector<InputStream>();
    dbProperties = new Vector<Properties>();

    try
    {
      // Get the properties for the database connections

      for ( int i = 0; i<Config.DatabasePropertiesSize; i++ )
      {
        dbProperties.addElement(new Properties() );
      }

      Integer myInt = new Integer(Config.DatabasePropertiesSize);

      buddy.println(myInt.toString());


      InputStream tempStream = null;
      for ( int i = 0; i<Config.DatabasePropertiesSize; i++ )
      {


        String tempStr =Config.DatabaseProperties[i];



        tempStream = new FileInputStream(tempStr);
        ((Properties) dbProperties.elementAt(i)).load(tempStream);
        inStreams.addElement( tempStream );
      }


    //add by qingyang, use a global DB connection pool
     synchronized(DBconnLOCK){
     	    if(poolInitiMarker == true){
     		  return;
     	    }else{
     		  poolInitiMarker = true;
     	    }

     	      // load the driver
     	    Class.forName(((Properties)dbProperties.elementAt(0)).getProperty("datasource.classname"));

     	    for ( int i = 0; i<Config.DatabasePropertiesSize;i++ )
     	    {
     	       databaseConnections.addElement( new Stack());
     	    }
     	    initializeConnections();
        }
    }
    catch (FileNotFoundException f)
    {
      throw new UnavailableException(
        "Couldn't find file mysql.properties: " + f + "<br>");
    }
    catch (IOException io)
    {
      throw new UnavailableException(
        "Cannot open read mysql.properties: " + io + "<br>");
    }
    catch (ClassNotFoundException c)
    {
      throw new UnavailableException(
        "Couldn't load database driver: " + c + "<br>");
    }
    catch (SQLException s)
    {
      throw new UnavailableException(
        "Couldn't get database connection: " + s + "<br>");
    }
    finally
    {
      try
      {
        InputStream in = null;
        for ( int i=0; i< Config.DatabasePropertiesSize; i++ )
        {
          in = ((InputStream) inStreams.elementAt(i));
          if ( in !=null )
            in.close();

        }

      }
      catch (Exception e)
      {
      }
    }
  }

  /**
   * Initialize the pool of connections to the database.
   * The caller must ensure that the driver has already been
   * loaded else an exception will be thrown.
   *
   * @exception SQLException if an error occurs
   */
  public synchronized void initializeConnections() throws SQLException
  {


    //This implementation must be changed once the databases are modularized away
    if (enablePooling)
    {
      Connection tempConn = null;
      Properties tempProp = null;
      for ( int j =0; j<Config.DatabasePropertiesSize; j++ )
      {
        tempProp=((Properties)dbProperties.elementAt(j));
        for (int i = 0; i < poolSize; i++)
        {

          // Get connections to the database
          tempConn =  DriverManager.getConnection(
            tempProp.getProperty("datasource.url"),
            tempProp.getProperty("datasource.username"),
            tempProp.getProperty("datasource.password"));

          connectionLookup.put(tempConn, new Integer(0));
          ((Stack) (databaseConnections.elementAt(0)) ).push(tempConn);
        }
      }
    }
  }

  /**
  * Closes a <code>Connection</code>.
  * @param connection to close
  */
   private void closeConnection(Connection connection)
   {
     try
     {
       connection.close();
     }
     catch (Exception e)
     {

     }
   }



  /**
   * Gets a connection from the pool that can do reading and writing (random)
   *
   * @return a <code>Connection</code> or
   * null if no connection is available
   */
  public synchronized Connection getRWConnection()
  {

    Stack tempStack= ((Stack)(databaseConnections.elementAt(readWriteIndex)) );

    if (enablePooling)
    {
      try
      {
        // Wait for a connection to be available
        while (tempStack.isEmpty())
        {
          try
          {
            wait();
          }
          catch (InterruptedException e)
          {
            System.out.println("Connection pool wait interrupted.");
          }
        }

        Connection c = (Connection) tempStack.pop();
        return c;
      }

      catch (EmptyStackException e)
      {
        System.out.println("Out of connections.");
        return null;
      }
    }
    /*
    else
    {
       try
       {
        return DriverManager.getConnection(
        dbProperties.getProperty("datasource.url"),
        dbProperties.getProperty("datasource.username"),
        dbProperties.getProperty("datasource.password"));
       }
       catch (SQLException ex)
       {
        ex.printStackTrace();
        return null;
       }
    }
    */
    return null;
  }



  /**
   * Gets a connection from the pool that can only be used for reading (round-robin)
   *
   * @return a <code>Connection</code> or
   * null if no connection is available
   */
  public  Connection getRConnection()
  {
    final int NumAttempts = 3;
    Stack tempStack = null;
    if (enablePooling)
    {
      for ( int i=0; i<NumAttempts; i++ )
      {
        //int randNum = generator.nextInt( Config.DatabasePropertiesSize );

        tempStack = ((Stack) (databaseConnections.elementAt(0) ));


        if ( tempStack.isEmpty()==false ){
          break;
	}
      }
      try
      {
        // Wait for a connection to be available
        while (tempStack.isEmpty())
        {
          try
          {
        	  synchronized(DBconnLOCK) {
				  DBconnLOCK.wait();
			  }
          }
          catch (InterruptedException e)
          {
            System.out.println("Connection pool wait interrupted.");
          }
        }

        Connection c = (Connection) tempStack.pop();
        return c;
      }

      catch (EmptyStackException e)
      {
        System.out.println("Out of connections.");
        return null;
      }
    }
    return null;
  }

  /**
   * Releases a read connection to the pool.
   *
   * @param c the connection to release
   */
  public  void releaseRConnection(Connection c )
  {
    if (enablePooling)
    {

      //int databaseIndex = ((Integer)connectionLookup.get(c)).intValue();
      boolean mustNotify = ((Stack) ( databaseConnections.elementAt(0) ) ).isEmpty();
      ( (Stack) (databaseConnections.elementAt(0) )).push(c);

      // Wake up one servlet waiting for a connection (if any)
      if (mustNotify){
		  synchronized(DBconnLOCK) {
			  DBconnLOCK.notifyAll();
		  }
      }
    }
    else
    {
      closeConnection(c);
    }
  }



  /**
   * Releases a read/write connection to the pool.
   *
   * @param c the connection to release
   */
  public synchronized void releaseRWConnection(Connection c )
  {
    if (enablePooling)
    {
      boolean mustNotify = ((Stack) ( databaseConnections.elementAt(readWriteIndex) ) ).isEmpty();
      ((Stack)(databaseConnections.elementAt(readWriteIndex) ) ).push(c);
      // Wake up one servlet waiting for a connection (if any)
      if (mustNotify)
        notifyAll();
    }
    else
    {
      closeConnection(c);
    }
  }






  /**
   * Release all the connections to the database.
   *
   * @exception SQLException if an error occurs
   */
  public synchronized void finalizeConnections() throws SQLException
  {
    if (enablePooling)
    {
      Connection c = null;
      for ( int i=0 ; i <  Config.DatabasePropertiesSize ; i++ )
      {

        while (  !((Stack) (databaseConnections.elementAt(i) )).isEmpty())
        {
          c = (Connection) ((Stack) (databaseConnections.elementAt(i) )).pop();
          c.close();
        }
      }
    }
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
    try
    {
      finalizeConnections();
    }
    catch (SQLException e)
    {
    }
  }

}
