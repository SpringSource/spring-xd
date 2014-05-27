/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.integration.bus;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;
import org.springframework.xd.dirt.integration.rabbit.RabbitMessageBus;


/**
 * Test support class for {@link RabbitMessageBus}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 */
public class RabbitTestMessageBus extends AbstractTestMessageBus {

	private final RabbitAdmin rabbitAdmin;

	public RabbitTestMessageBus(ConnectionFactory connectionFactory) {
		this.rabbitAdmin = new RabbitAdmin(connectionFactory);
	}

	public RabbitTestMessageBus(ConnectionFactory connectionFactory, MultiTypeCodec<Object> codec) {
		RabbitMessageBus messageBus = new RabbitMessageBus(connectionFactory, codec);
		GenericApplicationContext context = new GenericApplicationContext();
		context.refresh();
		messageBus.setApplicationContext(context);
		messageBus.setIntegrationEvaluationContext(new StandardEvaluationContext());
		this.setMessageBus(messageBus);
		this.rabbitAdmin = new RabbitAdmin(connectionFactory);
	}

	@Override
	public void cleanup() {
		if (!queues.isEmpty()) {
			for (String queue : queues) {
				rabbitAdmin.deleteQueue("xdbus." + queue);
				if (queue.contains("part")) {
					for (int i = 0; i < 10; i++) {
						rabbitAdmin.deleteQueue("xdbus." + queue + "-" + i);
					}
				}
				rabbitAdmin.deleteQueue("foo." + queue);
			}
		}
		if (!topics.isEmpty()) {
			for (String exchange : topics) {
				rabbitAdmin.deleteExchange("xdbus." + exchange);
			}
		}
	}
}
