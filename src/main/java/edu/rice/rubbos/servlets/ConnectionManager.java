package edu.rice.rubbos.servlets;

import java.util.Stack;
import java.util.Properties;
import java.util.EmptyStackException;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;

public class ConnectionManager {
    private Stack freeConnections = null;
    private static ConnectionManager connectionManager;
    private boolean doneInit = false;


    private ConnectionManager() {
        freeConnections = new Stack();
    }

    public static ConnectionManager getInstance() {
        if (connectionManager == null) {
            connectionManager = new ConnectionManager();
        }
        return connectionManager;
    }


    public void initializeConnections(int poolSize, Properties dbProperties) throws SQLException {
        if (!doneInit) {
            doneInit = true;
            String dburl = dbProperties.getProperty("datasource.url");
            String values[] = dburl.split(",");
            int dbsize = values.length;
            int loadBalance = poolSize/dbsize ;
            for (int i = 0; i < loadBalance; i++) {
                // Get connections to the database
                for(int j = 0; j < dbsize ; j ++) {
                    String url = values[j];
                    System.out.println(url);
                    freeConnections.push(
                        DriverManager.getConnection(
                                url,
                                dbProperties.getProperty("datasource.username"),
                                dbProperties.getProperty("datasource.password")));
                }

            }
        }
    }

    /**
     * Gets a connection from the pool (round-robin)
     *
     * @return a <code>Connection</code> or
     *         null if no connection is available
     */
    public Connection getConnection() {
        try {
            // Wait for a connection to be available
            while (freeConnections.isEmpty()) {
                System.out.println("I am waiting : " + Thread.currentThread().getId());
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    System.out.println("Connection pool wait interrupted.");
                }
            }

            return (Connection) freeConnections.pop();
        }

        catch (EmptyStackException e) {
            System.out.println("Out of connections.");
            return null;
        }
    }

    public void releaseConnection(Connection c) {
        boolean mustNotify = freeConnections.isEmpty();
        freeConnections.push(c);
        // Wake up one servlet waiting for a connection (if any)
        if (mustNotify)
            notifyAll();

    }

    public void finalizeConnections() throws SQLException {
        Connection c = null;
        while (!freeConnections.isEmpty()) {
            c = (Connection) freeConnections.pop();
            c.close();
        }
    }
}