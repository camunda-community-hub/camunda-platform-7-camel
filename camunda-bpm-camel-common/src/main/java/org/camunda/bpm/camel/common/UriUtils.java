package org.camunda.bpm.camel.common;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_CAMEL_URI_SCHEME;

import java.util.regex.Pattern;

public class UriUtils {

	public static String[] parseUri(String uri) {
		Pattern p1 = Pattern.compile(CAMUNDA_BPM_CAMEL_URI_SCHEME + ":(//)*");
		Pattern p2 = Pattern.compile("\\?.*");

		uri = p1.matcher(uri).replaceAll("");
		uri = p2.matcher(uri).replaceAll("");

		return uri.split("/");
	}

}
