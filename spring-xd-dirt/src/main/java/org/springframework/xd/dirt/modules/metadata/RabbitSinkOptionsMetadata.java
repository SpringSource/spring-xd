/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.springframework.xd.dirt.modules.metadata;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_STREAM_NAME;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Captures options to the {@code rabbit} sink module.
 * 
 * @author Eric Bottard
 */
@Mixin(RabbitConnectionMixin.class)
public class RabbitSinkOptionsMetadata {

	private String exchange = "";

	private String routingKey = "'" + XD_STREAM_NAME + "'";


	public String getExchange() {
		return exchange;
	}

	@ModuleOption("the Exchange on the RabbitMQ broker to which messages should be sent")
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}


	public String getRoutingKey() {
		return routingKey;
	}

	@ModuleOption("the routing key to be passed with the message, as a SpEL expression")
	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}

}
