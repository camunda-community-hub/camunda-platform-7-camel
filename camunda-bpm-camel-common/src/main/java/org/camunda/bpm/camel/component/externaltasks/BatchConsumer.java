package org.camunda.bpm.camel.component.externaltasks;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_DEFINITION_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_DEFINITION_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_INSTANCE_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_PRIO;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.ScheduledBatchPollingConsumer;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.util.CastUtils;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.camel.component.CamundaBpmExternalTaskEndpointImpl;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;

public class BatchConsumer extends ScheduledBatchPollingConsumer {

    private final CamundaBpmEndpoint camundaEndpoint;

    private int timeout;

    private final int retryTimeout;
    
    private final long lockDuration;

    private final String topic;

    public BatchConsumer(final CamundaBpmEndpoint endpoint, final Processor processor,
    		final int retryTimeout, final long lockDuration, final String topic) {

        super(endpoint, processor);

        this.camundaEndpoint = endpoint;
        this.retryTimeout = retryTimeout;
        this.lockDuration = lockDuration;
        this.topic = topic;

    }

    public BatchConsumer(final CamundaBpmEndpoint endpoint, final Processor processor,
            final ScheduledExecutorService executor, final int retryTimeout,
            final long lockDuration, final String topic) {

        super(endpoint, processor, executor);

        this.camundaEndpoint = endpoint;
        this.retryTimeout = retryTimeout;
        this.lockDuration = lockDuration;
        this.topic = topic;

    }

    @Override
    public int processBatch(Queue<Object> exchanges) throws Exception {

        int total = exchanges.size();
        int answer = total;

        for (int index = 0; index < total && isBatchAllowed(); index++) {
            // only loop if we are started (allowed to run)
            // use poll to remove the head so it does not consume memory even
            // after we have processed it
            Exchange exchange = (Exchange) exchanges.poll();
            // add current index and total as properties
            exchange.setProperty(Exchange.BATCH_INDEX, index);
            exchange.setProperty(Exchange.BATCH_SIZE, total);
            exchange.setProperty(Exchange.BATCH_COMPLETE, index == total - 1);

            // update pending number of exchanges
            pendingExchanges = total - index - 1;

            boolean started = processExchange(exchange);

            // if we did not start processing then decrement the counter
            if (!started) {
                answer--;
            }
        }

        // drain any in progress files as we are done with this batch
        removeExcessiveInProgressTasks(CastUtils.cast((Queue<?>) exchanges, Exchange.class), 0);

        return answer;

    }

    private boolean processExchange(final Exchange exchange) throws Exception {

    	exchange.addOnCompletion(new Synchronization() {
			
			@Override
			public void onFailure(Exchange exchange) {
				completeTask(exchange);
			}
			
			@Override
			public void onComplete(Exchange exchange) {
				completeTask(exchange);
			}
		});
    	
    	getProcessor().process(exchange);
    	return true;

        /*
         * 
         * result.addOnCompletion(new Synchronization() {
         * 
         * @Override public void onFailure(Exchange exchange) {
         * System.err.println("failure"); }
         * 
         * @Override public void onComplete(Exchange exchange) { try {
         * Thread.sleep(1000); } catch (InterruptedException e) { // TODO
         * Auto-generated catch block e.printStackTrace(); } System.err.println(
         * "done (" + topic + "): " + Thread.currentThread().toString()); } });
         * 
         */

    }

    /**
     * Drain any in progress files as we are done with this batch
     *
     * @param exchanges
     *            the exchanges
     * @param limit
     *            the limit
     */
    protected void removeExcessiveInProgressTasks(Queue<Exchange> exchanges, int limit) {

        while (exchanges.size() > limit) {
            // must remove last
            Exchange exchange = exchanges.poll();
            releaseTask(exchange);
        }

    }

    private void releaseTask(final Exchange exchange) {

        exchange.setProperty(Exchange.ROLLBACK_ONLY, Boolean.TRUE);
        completeTask(exchange);

    }

    private ExternalTaskService getExternalTaskService() {

        return camundaEndpoint.getProcessEngine().getExternalTaskService();

    }

    @SuppressWarnings("unchecked")
	private void completeTask(final Exchange exchange) {

        final Message in = exchange.getIn();
        if (in == null) {
            throw new RuntimeCamelException("Unexpected exchange: in is null!");
        }

        final LockedExternalTask task = in.getHeader(CamundaBpmExternalTaskEndpointImpl.EXCHANGE_HEADER_TASK,
        		LockedExternalTask.class);
        if (task == null) {
            throw new RuntimeCamelException("Unexpected exchange: in-header '"
                    + CamundaBpmExternalTaskEndpointImpl.EXCHANGE_HEADER_TASK + "' is null!");
        }

        final ExternalTaskService externalTaskService = getExternalTaskService();

        // rollback
        if (exchange.isRollbackOnly()) {

            externalTaskService.unlock(task.getId());

        } else
        // failure
        if (exchange.isFailed()) {

            final Exception exception = exchange.getException();
            externalTaskService.handleFailure(task.getId(),
                    task.getWorkerId(),
                    exception.getMessage(),
                    task.getRetries(),
                    retryTimeout);

        } else
        // success
        {

        	final Message out = exchange.getOut();
        	
            final Map<String, Object> variablesToBeSet;
            if ((out != null)
            		&& (out.getBody() != null)
            		&& (out.getBody() instanceof Map)) {
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
static int pollCounter = 0;
    protected int poll() throws Exception {

        int messagesPolled = 0;
        pollCounter++;
        System.err.println("polled: " + pollCounter);

        PriorityQueue<Exchange> exchanges = new PriorityQueue<Exchange>(new Comparator<Exchange>() {
            @Override
            public int compare(Exchange o1, Exchange o2) {
            	Long prio1 = (Long) o1.getProperty(CAMUNDA_BPM_PROCESS_PRIO, 0);
            	Long prio2 = (Long) o2.getProperty(CAMUNDA_BPM_PROCESS_PRIO, 0);
                return prio1.compareTo(prio2);
            }
        });

        if (isPollAllowed()) {
        	
	        final List<LockedExternalTask> tasks = getExternalTaskService()
	        		.fetchAndLock(maxMessagesPerPoll, camundaEndpoint.getEndpointUri(), true)
	        		.topic(topic, lockDuration)
	        		.execute();
        
            messagesPolled = tasks.size();

            for (final LockedExternalTask task : tasks) { 

                Exchange exchange = getEndpoint().createExchange();
                exchange.setFromEndpoint(getEndpoint());
                exchange.setExchangeId(task.getWorkerId() + "/" + task.getId());
                exchange.setProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID, task.getProcessInstanceId());
                exchange.setProperty(CAMUNDA_BPM_PROCESS_DEFINITION_KEY, task.getProcessDefinitionKey());
                exchange.setProperty(CAMUNDA_BPM_PROCESS_DEFINITION_ID, task.getProcessDefinitionId());
                exchange.setProperty(CAMUNDA_BPM_PROCESS_PRIO, task.getPriority());
                
                // result.setProperty(BatchConsumer.PROPERTY_PRIORITY, ???);

                final Message in = exchange.getIn();
                in.setHeader(CamundaBpmExternalTaskEndpointImpl.EXCHANGE_HEADER_TASK, task);
            	
            	// if the result of the polled exchange has output we should create
	            // a new exchange and
	            // use the output as input to the next processor
	            if (exchange.hasOut()) {
	                // lets create a new exchange
	                Exchange newExchange = getEndpoint().createExchange();
	                newExchange.getIn().copyFrom(exchange.getOut());
	                exchange = newExchange;
	            }
	
	            exchanges.add(exchange);
	            
            }

        }

        processBatch(CastUtils.cast(exchanges));

        return messagesPolled;

    }

    public int getTimeout() {

        return timeout;

    }

    /**
     * Sets a timeout to use with {@link PollingConsumer}. <br/>
     * <br/>
     * Use <tt>timeout < 0</tt> for {@link PollingConsumer#receive()}. <br/>
     * Use <tt>timeout == 0</tt> for {@link PollingConsumer#receiveNoWait()}.
     * <br/>
     * Use <tt>timeout > 0</tt> for {@link PollingConsumer#receive(long)}}.
     * <br/>
     * The default timeout value is <tt>0</tt>
     *
     * @param timeout
     *            the timeout value
     */
    public void setTimeout(int timeout) {

        this.timeout = timeout;

    }

}
