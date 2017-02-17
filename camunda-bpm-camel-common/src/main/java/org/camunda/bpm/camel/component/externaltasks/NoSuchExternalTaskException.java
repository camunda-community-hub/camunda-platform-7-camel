package org.camunda.bpm.camel.component.externaltasks;

import org.apache.camel.RuntimeCamelException;

public class NoSuchExternalTaskException extends RuntimeCamelException {

    private static final long serialVersionUID = 1L;

    public NoSuchExternalTaskException() {
        // nothing to do
    }

    public NoSuchExternalTaskException(String message) {
        super(message);
    }

    public NoSuchExternalTaskException(Throwable cause) {
        super(cause);
    }

    public NoSuchExternalTaskException(String message, Throwable cause) {
        super(message, cause);
    }

}
