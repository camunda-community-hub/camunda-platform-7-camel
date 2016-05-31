package org.camunda.bpm.camel.component.externaltasks;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.impl.PollingConsumerSupport;
import org.apache.camel.spi.Synchronization;

public class PollingConsumer extends PollingConsumerSupport {

    private final String topic;

    private int done = 0;

    public PollingConsumer(final Endpoint endpoint, final String topic) {
        super(endpoint);
        this.topic = topic;
    }

    @Override
    public Exchange receive() {
        return receive(-1);
    }

    @Override
    public Exchange receiveNoWait() {
        return receive(0);
    }

    @Override
    public Exchange receive(long timeout) {

        if (done == 2)
            return null;

        System.err.println("receive (" + topic + "): " + Thread.currentThread().toString());

        final Exchange result = getEndpoint().createExchange(ExchangePattern.InOnly);
        result.setExchangeId(Long.toString(System.currentTimeMillis()));
        result.setFromEndpoint(getEndpoint());
        final Message in = result.getIn();
        in.setBody(topic);

        result.addOnCompletion(new Synchronization() {

            @Override
            public void onFailure(Exchange exchange) {
                System.err.println("failure");
            }

            @Override
            public void onComplete(Exchange exchange) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.err.println("done (" + topic + "): " + Thread.currentThread().toString());
            }
        });

        done++;

        return result;

    }

    @Override
    protected void doStart() throws Exception {

        // TODO Auto-generated method stub
        System.err.println("doStart();");

    }

    @Override
    protected void doStop() throws Exception {

        // TODO Auto-generated method stub

    }

}
