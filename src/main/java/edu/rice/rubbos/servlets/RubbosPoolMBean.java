package edu.rice.rubbos.servlets;

public interface RubbosPoolMBean{

         int getPool();
     boolean setPool(int pool);
     boolean setTcat(int tcat,String mysqls);
	 boolean addMysql(String host, int port);
	 boolean rmMysql(String host);
}


