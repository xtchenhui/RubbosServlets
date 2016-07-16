package edu.rice.rubbos.servlets;

public interface RubbosPoolMBean{

         int getPool();
	 boolean changeDBpool(int newsize) throws Exception;
}


