package org.camunda.bpm.camel.blueprint;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class OsgiHelper {

	@SuppressWarnings("unchecked")
	public static <T> T getService(Class<T> clazz) {
		BundleContext context = FrameworkUtil.getBundle(clazz).getBundleContext();
		ServiceReference serviceReference = context.getServiceReference(clazz.getName());
		return (T)context.getService(serviceReference);
	}
}
