package org.camunda.bpm.camel.component.externaltasks;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.ScheduledBatchPollingConsumer;
import org.apache.camel.util.CastUtils;
import org.apache.camel.util.ServiceHelper;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.camel.component.CamundaBpmExternalTaskEndpointImpl;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.ExternalTask;

public class BatchConsumer extends ScheduledBatchPollingConsumer {

    private static final String PROPERTY_PRIORITY = "camunda.prio";

    private final CamundaBpmEndpoint camundaEndpoint;

    private PollingConsumer pollingConsumer;

    private int timeout;

    private int retryTimeout;

    public BatchConsumer(final CamundaBpmEndpoint endpoint, final Processor processor, final int retryTimeout) {

        super(endpoint, processor);

        this.camundaEndpoint = endpoint;
        this.retryTimeout = retryTimeout;

    }

    public BatchConsumer(final CamundaBpmEndpoint endpoint, final Processor processor,
            final ScheduledExecutorService executor, final int retryTimeout) {

        super(endpoint, processor, executor);

        this.camundaEndpoint = endpoint;
        this.retryTimeout = retryTimeout;

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

            // if we did not start process the file then decrement the counter
            if (!started) {
                answer--;
            }
        }

        // drain any in progress files as we are done with this batch
        removeExcessiveInProgressTasks(CastUtils.cast((Queue<?>) exchanges, Exchange.class), 0);

        return answer;

    }

    private boolean processExchange(final Exchange exchange) throws Exception {

        if (getProcessor() instanceof AsyncProcessor) {

            AsyncProcessor asyncProcessor = (AsyncProcessor) getProcessor();
            asyncProcessor.process(exchange, new AsyncCallback() {
                @Override
                public void done(boolean doneSync) {
                    completeTask(exchange);
                }
            });

        } else {

            getProcessor().process(exchange);
            completeTask(exchange);

        }

        return false;

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

    private void completeTask(final Exchange exchange) {

        final Message out = exchange.getOut();
        if (out == null) {
            throw new RuntimeCamelException("Unexpected exchange: out is null!");
        }

        final ExternalTask task = out.getHeader(CamundaBpmExternalTaskEndpointImpl.EXCHANGE_HEADER_TASK,
                ExternalTask.class);
        if (task == null) {
            throw new RuntimeCamelException("Unexpected exchange: out-header '"
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

            @SuppressWarnings("unchecked")
            final Map<String, Object> variablesToBeSet = out.getBody(Map.class);

            if (variablesToBeSet != null) {
                externalTaskService.complete(task.getId(), task.getWorkerId(), variablesToBeSet);
            } else {
                externalTaskService.complete(task.getId(), task.getWorkerId());
            }

        }

    }

    private boolean isBatchComplete(final int numberOfItems) {

        if (maxMessagesPerPoll > 0) {

            return numberOfItems == maxMessagesPerPoll;

        }

        return false;

    }

    protected int poll() throws Exception {

        int messagesPolled = 0;

        PriorityQueue<Exchange> exchanges = new PriorityQueue<Exchange>(new Comparator<Exchange>() {
            @Override
            public int compare(Exchange o1, Exchange o2) {
                Integer prio1 = (Integer) o1.getProperty(BatchConsumer.PROPERTY_PRIORITY, 0);
                Integer prio2 = (Integer) o2.getProperty(BatchConsumer.PROPERTY_PRIORITY, 0);
                return prio1.compareTo(prio2);
            }
        });

        while (isBatchComplete(messagesPolled) && isPollAllowed()) {

            Exchange exchange;
            if (timeout == 0) {
                exchange = pollingConsumer.receiveNoWait();
            } else if (timeout < 0) {
                exchange = pollingConsumer.receive();
            } else {
                exchange = pollingConsumer.receive(timeout);
            }

            if (exchange == null) {
                break;
            }

            messagesPolled++;
            log.trace("Polled {} {}", messagesPolled, exchange);

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

    @Override
    protected void doStart() throws Exception {

        pollingConsumer = getEndpoint().createPollingConsumer();
        ServiceHelper.startService(pollingConsumer);
        super.doStart();

    }

    @Override
    protected void doStop() throws Exception {

        ServiceHelper.stopService(pollingConsumer);
        super.doStop();

    }

}
