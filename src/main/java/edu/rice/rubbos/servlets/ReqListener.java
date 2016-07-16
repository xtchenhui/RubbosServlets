package edu.rice.rubbos.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;

import java.util.Hashtable;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class ReqListener implements ServletRequestListener
{
    private final int OUTPUT_FREQUENCY = 20;
    private final int SAMPLING_RATIO = 10;

    private Hashtable ht;
    private int count = 0;
    private StringBuffer sb;

    public ReqListener()
    {
	ht = new Hashtable(200);
	sb = new StringBuffer();
    }

    public void requestInitialized(ServletRequestEvent sre)
    {
	Long start_time = new Long(System.nanoTime());
	String threadName = Thread.currentThread().getName();
	ht.put(threadName, start_time);
    }
    
    public void requestDestroyed(ServletRequestEvent sre)
    {
	Long end_time = new Long(System.nanoTime());
	// Sampling only (SAMPLING_RATIO)%
	if ((end_time / 1000) % (int)(100 / SAMPLING_RATIO) != 0) {
	    return;
	}

	String threadName = Thread.currentThread().getName();
	Long start_time = (Long)ht.get(threadName);
	if (start_time == null) {
	    return;
	}
	long time_micro = (long)((end_time.longValue() - start_time.longValue()) / 1000); // micro seconds

	// Calendar cal = new GregorianCalendar();
	// String tsString = String.format("%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS", cal);
	long ts = System.currentTimeMillis();
	String tsString = String.format("%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS.%1$tL", ts);
	//used for logging purposes
	ServletContext context = sre.getServletContext();
	//Used to get information about a new request
	ServletRequest request = sre.getServletRequest();
	StringBuffer outputSB = null;
	synchronized (this) {
	    count ++;
	    sb.append("\n" + tsString + " " + "ThreaD:" + Thread.currentThread().getName() + " " +
		      (request instanceof HttpServletRequest ?
		       ((HttpServletRequest)request).getRequestURI() : "Unknown") +
		      " " + time_micro + " ST(nano)=" + start_time + " ET(nano)=" + end_time);
	    // output only once per (OUTPUT_FREQUENCY)
	    if (count == OUTPUT_FREQUENCY) {
		outputSB = sb;
		sb = new StringBuffer();
		count = 0;
	    }
	    else {
		return;
	    }
	}
	context.log(outputSB.toString());
	/*
	synchronized (context){
	context.log(ts + " " + "ThreaD:" + Thread.currentThread().getName() + " " +
		    (request instanceof HttpServletRequest ?
		     ((HttpServletRequest)request).getRequestURI() : "Unknown") +
		    " " + time_micro);
	}//synchronized
	*/
    }//requestDestroyed
}// ReqListener

