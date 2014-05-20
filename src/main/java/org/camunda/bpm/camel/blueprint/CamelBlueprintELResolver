package org.camunda.bpm.camel.blueprint;

import java.lang.reflect.Method;

import org.camunda.bpm.engine.impl.javax.el.ELContext;
import org.camunda.bpm.extension.osgi.blueprint.BlueprintELResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

public class CamelBlueprintELResolver extends BlueprintELResolver {

	public static final String CAMEL_EL_ID = "camel";

	private static final Logger LOGGER = LoggerFactory.getLogger(CamelBlueprintELResolver.class);

	private CamelServiceImpl camelService;

	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		// according to javadoc, can only be a String
		String key = (String)property;
		if (CAMEL_EL_ID.equals(key)) {
			context.setPropertyResolved(true);
			return getCamelService();
		}
		else {
			return super.getValue(context, base, property);
		}
	}

	@Override
	public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
		if (base != null && base.getClass() == CamelServiceImpl.class) {
			CamelServiceImpl camelService = (CamelServiceImpl)base;
			@SuppressWarnings("rawtypes")
			Class[] paramClasses = new Class[params.length];
			StringBuffer p = new StringBuffer();
			for (int i = 0; i < params.length; i++) {
				p.append(params[i].getClass().getName() + " ");
				paramClasses[i] = params[i].getClass();
			}
			try {
				Method m = BeanUtils.findMethod(camelService.getClass(), (String)method, paramClasses);
				if (m == null) {
					LOGGER.warn("Failed to find method: " + method + " on " + base.getClass() + " with parameters: " + p.toString());
					context.setPropertyResolved(false);
					return null;
				}
				context.setPropertyResolved(true);
				return m.invoke(camelService, params);
			}
			catch (Exception e) {
				LOGGER.warn("Failed to invoke method: " + method + " on " + base.getClass() + " with parameters: " + p.toString()
						+ ". Reason: " + e.getMessage());
				LOGGER.debug("Details: ", e);
				context.setPropertyResolved(false);
			}
		}
		return null;
	}

	private CamelServiceImpl getCamelService() {
		if (camelService == null) {
			camelService = new CamelServiceImpl();
		}
		return camelService;
	}

}
