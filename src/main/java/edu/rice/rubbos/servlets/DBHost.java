package edu.rice.rubbos.servlets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
public class DBHost {
	
	
	private String Host;
	
	private int port;
	/**
	 * used for database source connection
	 */
	private String Username;
	
	private String Passwd;
	
	private String datasource;
	
	private ConcurrentLinkedQueue<Connection> connque = new ConcurrentLinkedQueue<Connection>();

	public DBHost(String hostname, int port, String username, String passwd, int conns){
		this.Host = hostname;
		this.Username = username;
		this.Passwd = passwd;
		this.port = port;
		this.datasource = "jdbc:mysql://"+this.Host+":"+this.port+"/rubbos";
		init(conns);
	}
	
	

	
	public void init(int conns){
		
		for(int i = 0; i < conns; i++){
			try {
				Connection tmp = DriverManager.getConnection(this.datasource, this.Username, this.Passwd);
				this.connque.offer(tmp);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}		
	}
	
	public int getConnSize(){
		return this.connque.size();
	}
	
	
	public boolean reduceConn(int reduce){		
		while(reduce > 0){
			while(this.connque.size() > 0 && reduce > 0){
				try {
					this.connque.poll().close();
					reduce--;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if(reduce > 0 && this.connque.size() == 0){
				try {
					this.connque.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	public boolean addConn(int add){
		for(int i = 0; i < add; i++){
			try {
				Connection tmp = DriverManager.getConnection(this.datasource, this.Username, this.Passwd);
				this.connque.offer(tmp);
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}		
		return true;
	}
	
	public Connection getConnection(){
		if(this.connque.size() == 0){
			try {
				this.connque.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return connque.poll();
	}
	
	public void ReleaseConn(Connection conn){
		this.connque.offer(conn);
		this.connque.notifyAll();		
	}
	
	public void finalizedConnections(){
		for(Connection conn:this.connque){
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
