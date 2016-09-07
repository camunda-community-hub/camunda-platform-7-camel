package org.camunda.bpm.camel.component.externaltasks;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.Synchronization;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.camel.component.CamundaBpmPollExternalTasksEndpointImpl;
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

    @SuppressWarnings("unchecked")
    void internalProcessing(final Exchange exchange) {

        final Message in = exchange.getIn();
        if (in == null) {
            throw new RuntimeCamelException("Unexpected exchange: in is null!");
        }

        final ExternalTaskService externalTaskService = getExternalTaskService();

        final LockedExternalTask lockedTask = in.getHeader(CamundaBpmPollExternalTasksEndpointImpl.EXCHANGE_HEADER_TASK,
                LockedExternalTask.class);
        final String lockedTaskId = in.getHeader(CamundaBpmPollExternalTasksEndpointImpl.EXCHANGE_HEADER_TASKID,
                String.class);

        final String taskId;
        if (lockedTask != null) {
            taskId = lockedTask.getId();
        } else if (lockedTaskId != null) {
            taskId = lockedTaskId;
        } else {
            throw new RuntimeCamelException("Unexpected exchange: in-header '"
                    + CamundaBpmPollExternalTasksEndpointImpl.EXCHANGE_HEADER_TASK + "' and '"
                    + CamundaBpmPollExternalTasksEndpointImpl.EXCHANGE_HEADER_TASKID + "' is null!");
        }

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

        // 'process-externalTask' at the end of a route does not have any output
        // - only input which was any proceeder's output.
        final Message out;
        if (exchange.hasOut()) {
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

            final int retries;
            if (task.getRetries() == null) {
                retries = this.retries;
            } else {
                retries = task.getRetries() - 1;
            }

            final long calculatedTimeout = calculateTimeout(retries);

            final Exception exception = exchange.getException();
            externalTaskService.handleFailure(task.getId(),
                    task.getWorkerId(),
                    exception.getMessage(),
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

    private long calculateTimeout(final int retries) {

        final int currentTry = this.retries - retries;
        if (retries < 1) {
            return 0;
        } else if ((retryTimeouts != null) && (currentTry < retryTimeouts.length)) {
            return retryTimeouts[currentTry];
        } else {
            return retryTimeout;
        }

    }

}
