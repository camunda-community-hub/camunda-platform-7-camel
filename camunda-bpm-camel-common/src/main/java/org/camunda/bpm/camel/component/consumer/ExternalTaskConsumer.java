/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.camel.component.consumer;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.TOPIC_PARAMETER;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultScheduledPollConsumer;

/**
 * Consumes external tasks of the configured topic
 * <p/>
 * Example: camunda-bpm://externalTask?topic=aTopic
 *
 * @author Stephan Pelikan
 */
public class ExternalTaskConsumer extends DefaultScheduledPollConsumer implements CamundaBpmConsumer, PollingConsumer {

	private final String topic;
	
    public ExternalTaskConsumer(DefaultEndpoint defaultEndpoint, Processor processor, Map<String, Object> parameters) {
        super(defaultEndpoint, processor);

		if (parameters.containsKey(TOPIC_PARAMETER)) {
			this.topic = (String) parameters.get(TOPIC_PARAMETER);
		} else {
			throw new IllegalArgumentException("You need to pass the '"
					+ TOPIC_PARAMETER + "' parameter! Parameters received: "
					+ parameters);
		}
    }

    public ExternalTaskConsumer(Endpoint endpoint, Processor processor, ScheduledExecutorService executor, Map<String, Object> parameters) {
        super(endpoint,processor, executor);

		if (parameters.containsKey(TOPIC_PARAMETER)) {
			this.topic = (String) parameters.remove(TOPIC_PARAMETER);
		} else {
			throw new IllegalArgumentException("You need to pass the '"
					+ TOPIC_PARAMETER + "' parameter! Parameters received: "
					+ parameters);
		}
    }

    public ExternalTaskConsumer(DefaultEndpoint endpoint, Processor processor, String topic) {
        super(endpoint, processor);
        this.topic = topic;
    }

    public ExternalTaskConsumer(Endpoint endpoint, Processor processor, ScheduledExecutorService executor, String topic) {
        super(endpoint, processor, executor);
        this.topic = topic;
    }
    
    @Override
    protected void doStart() throws Exception {
      //((CamundaBpmExternalTaskEndpoint) getEndpoint()).addConsumer(this);
      super.doStart();
    }

	@Override
	public Exchange receive() {
		System.err.println("receive()");
		return null;
	}

	@Override
	public Exchange receiveNoWait() {
		System.err.println("receiveNoWait()");
		
		final Exchange result = getEndpoint().createExchange(ExchangePattern.InOut);
		result.setExchangeId(Long.toString(System.currentTimeMillis()));
		result.setFromEndpoint(getEndpoint());
		result.setFromRouteId(getRoute().getId());
		final Message in = result.getIn();
		in.setBody("JUHU");
		
		return result;
	}

	@Override
	public Exchange receive(long timeout) {
		System.err.println("receive(timeout)");
		return null;
	}
    
    
	
}