package edu.rice.rubbos.servlets;

public interface RubbosPoolMBean{

         int getPool();
	 boolean changeDBpool(int newsize, int index) throws Exception;
	 boolean addMysql(int index);
	 boolean rmMysql(int index);
	 boolean addTcat(int index);
}


