package edu.rice.rubbos.servlets;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class RegisterMBeansListener implements ServletContextListener {

	private ObjectName objectName;

    public RegisterMBeansListener() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    	System.out.println("Registering MBean...");
    	final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    	try {
    		objectName = new ObjectName("RubbosLSU:type=PoolMon");
    		final RubbosPoolMBean mbean = new RubbosPool();
    		server.registerMBean(mbean, objectName);
    		System.out.println("MBean registered: " + objectName);
    	} catch (MalformedObjectNameException mone) {
    		mone.printStackTrace();
    	} catch (InstanceAlreadyExistsException iaee) {
			iaee.printStackTrace();
		} catch (MBeanRegistrationException mbre) {
			mbre.printStackTrace();
		} catch (NotCompliantMBeanException ncmbe) {
			ncmbe.printStackTrace();
		}
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    	System.out.println("Unregistering MBean...");
    	final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    	try {
    		objectName = new ObjectName("RubbosLSU:type=PoolMon");
    		server.unregisterMBean(objectName);
    		System.out.println("MBean unregistered: " + objectName);
    	} catch (MalformedObjectNameException mone) {
    		mone.printStackTrace();
    	} catch (MBeanRegistrationException mbre) {
			mbre.printStackTrace();
		} catch (InstanceNotFoundException infe) {
			infe.printStackTrace();
		}
    }
}
