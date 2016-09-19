package org.camunda.bpm.camel.component.externaltasks;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_ATTEMPTSSTARTED;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_RETRIESLEFT;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_TASK;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_TASKID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_RESPONSE_IGNORE;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.Synchronization;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;

public class TaskProcessor implements Processor {

    private static final Logger logger = Logger.getLogger(TaskProcessor.class.getCanonicalName());

    private final CamundaBpmEndpoint camundaEndpoint;

    // parameters
    private final int retries;
    private final long retryTimeout;
    private final long[] retryTimeouts;
    private final boolean completeTask;
    private final boolean onCompletion;
    private final String topic;
    private final String workerId;

    public TaskProcessor(final CamundaBpmEndpoint endpoint, final String topic, final int retries,
            final long retryTimeout, final long[] retryTimeouts, final boolean completeTask, final boolean onCompletion,
            final String workerId) {

        this.camundaEndpoint = endpoint;
        this.retries = retries;
        this.retryTimeout = retryTimeout;
        this.retryTimeouts = retryTimeouts;
        this.completeTask = completeTask;
        this.onCompletion = onCompletion;
        this.workerId = workerId;
        this.topic = topic;

    }

    private ExternalTaskService getExternalTaskService() {

        return camundaEndpoint.getProcessEngine().getExternalTaskService();

    }

    @Override
    public void process(final Exchange exchange) {

        if (!onCompletion) {

            internalProcessing(exchange);

        } else {

            // set headers only for "on-completion" processing because
            // otherwise the service doing something is already done
            // and therefore cannot use this in-headers any more.
            setInHeaders(exchange);

            final TaskProcessor taskProcessor = this;
            exchange.addOnCompletion(new Synchronization() {

                @Override
                public void onFailure(final Exchange exchange) {
                    taskProcessor.internalProcessing(exchange);
                }

                @Override
                public void onComplete(final Exchange exchange) {
                    taskProcessor.internalProcessing(exchange);
                }

            });

        }

    }

    private void setInHeaders(final Exchange exchange) {

        final Message in = getInMessage(exchange);
        final String taskId = getExternalTaskId(in);
        final ExternalTask task = getExternalTask(taskId);

        final int retries = retriesLeft(task.getRetries());
        final int attemptsStarted = attemptsStarted(task.getRetries());

        in.setHeader(EXCHANGE_HEADER_RETRIESLEFT, retries);
        in.setHeader(EXCHANGE_HEADER_ATTEMPTSSTARTED, attemptsStarted);

    }

    @SuppressWarnings("unchecked")
    void internalProcessing(final Exchange exchange) {

        final Message in = getInMessage(exchange);
        final String taskId = getExternalTaskId(in);
        final ExternalTask task = getExternalTask(taskId);

        final ExternalTaskService externalTaskService = getExternalTaskService();

        final Message out;
        if (onCompletion) {
            // 'process-externalTask' at the end of a route does not have any
            // output
            // - only input which was any proceeder's output.
            out = exchange.getOut();
        } else {
            out = in;
        }

        // failure
        if (exchange.isFailed()) {

            if (task == null) {
                logger.warning(
                        "Processing failed but the task seems to be already processed - will do nothing! Camnda external task id: '"
                                + taskId + "'");
                return;
            }

            final int retries = retriesLeft(task.getRetries());
            final long calculatedTimeout = calculateTimeout(task.getRetries());

            final Exception exception = exchange.getException();
            externalTaskService.handleFailure(task.getId(),
                    task.getWorkerId(),
                    exception != null ? exception.getMessage() : "task failed",
                    retries,
                    calculatedTimeout);

        } else
        // bpmn error
        if ((out != null) && (out.getBody() != null) && (out.getBody() instanceof String)) {

            final String errorCode = out.getBody(String.class);

            if (task == null) {
                logger.warning("Should complete the external task with BPM error '" + errorCode
                        + "' but the task seems to be already processed - will do nothing! Camnda external task id: '"
                        + taskId + "'");
                return;
            }

            // Ignore if service advises us to do so.
            if (errorCode.equals(EXCHANGE_RESPONSE_IGNORE)) {
                return;
            }

            externalTaskService.handleBpmnError(task.getId(), task.getWorkerId(), errorCode);

        } else
        // success
        if (completeTask) {

            if (task == null) {
                logger.warning("Should complete the external task but the task seems to be "
                        + "already processed - will do nothing! Camnda external task id: '" + taskId + "'");
                return;
            }

            final Map<String, Object> variablesToBeSet;
            if ((out != null) && (out.getBody() != null) && (out.getBody() instanceof Map)) {
                variablesToBeSet = out.getBody(Map.class);
            } else {
                variablesToBeSet = null;
            }

            if (variablesToBeSet != null) {
                externalTaskService.complete(task.getId(), task.getWorkerId(), variablesToBeSet);
            } else {
                externalTaskService.complete(task.getId(), task.getWorkerId());
            }

        }

    }

    private Message getInMessage(final Exchange exchange) {
        final Message in = exchange.getIn();
        if (in == null) {
            throw new RuntimeCamelException("Unexpected exchange: in is null!");
        }
        return in;
    }

    private ExternalTask getExternalTask(final String taskId) {

        final ExternalTask task = getExternalTaskService().createExternalTaskQuery().externalTaskId(
                taskId).singleResult();
        if (task != null) {
            if ((task.getWorkerId() != null) && (workerId != null) && !task.getWorkerId().equals(workerId)) {
                throw new RuntimeCamelException(
                        "Unexpected exchange: the external task '" + taskId + "' is locked for worker '"
                                + task.getWorkerId() + "' which differs from the configured worker '" + workerId + "!");
            }
            if ((task.getTopicName() != null) && (topic != null) && !task.getTopicName().equals(topic)) {
                throw new RuntimeCamelException(
                        "Unexpected exchange: the external task '" + taskId + "' is from topic '" + task.getWorkerId()
                                + "' which differs from the configured topic '" + topic + "!");
            }
        }
        return task;

    }

    private String getExternalTaskId(final Message in) {

        final LockedExternalTask lockedTask = in.getHeader(EXCHANGE_HEADER_TASK, LockedExternalTask.class);
        final String lockedTaskId = in.getHeader(EXCHANGE_HEADER_TASKID, String.class);

        final String taskId;

        if (lockedTask != null) {
            taskId = lockedTask.getId();
        } else if (lockedTaskId != null) {
            taskId = lockedTaskId;
        } else {
            throw new RuntimeCamelException("Unexpected exchange: in-header '" + EXCHANGE_HEADER_TASK + "' and '"
                    + EXCHANGE_HEADER_TASKID + "' is null!");
        }

        return taskId;

    }

    public int retriesLeft(final Integer taskRetries) {

        if (taskRetries == null) {
            return this.retries;
        } else {
            return taskRetries - 1;
        }

    }

    public int attemptsStarted(final Integer taskRetries) {

        final int retriesLeft = retriesLeft(taskRetries);
        return this.retries - retriesLeft;

    }

    private long calculateTimeout(final Integer taskRetries) {

        final int currentTry = attemptsStarted(taskRetries);
        if (retries < 1) {
            return 0;
        } else if ((retryTimeouts != null) && (currentTry < retryTimeouts.length)) {
            return retryTimeouts[currentTry];
        } else {
            return retryTimeout;
        }

    }

}
