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

package org.springframework.xd.dirt.listener;

import java.lang.management.ManagementFactory;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.xd.dirt.core.Container;
import org.springframework.xd.dirt.event.ContainerStartedEvent;
import org.springframework.xd.dirt.event.ContainerStoppedEvent;
import org.springframework.xd.dirt.listener.util.BannerUtils;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Gunnar Hillert
 */
public class RedisContainerEventListener extends AbstractContainerEventListener {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	public RedisContainerEventListener(RedisConnectionFactory redisConnectionFactory) {
		this.redisTemplate.setConnectionFactory(redisConnectionFactory);
		this.redisTemplate.afterPropertiesSet();
	}

	@Override
	protected void onContainerStartedEvent(ContainerStartedEvent event) {
		if (logger.isInfoEnabled()) {

			final String name = ManagementFactory.getRuntimeMXBean().getName();
			final Container container = event.getSource();
			this.redisTemplate.boundHashOps("containers").put(container.getId(), name);

			final Properties redisInfo = this.redisTemplate.getConnectionFactory().getConnection().info();

			final StringBuilder sb = new StringBuilder(OsUtils.LINE_SEPARATOR);

			sb.append(BannerUtils.getBanner())
				.append(String.format("Using Redis v%s (Mode: %s) on Port %s ",
						redisInfo.getProperty("redis_version"),
						redisInfo.getProperty("redis_mode"),
						redisInfo.getProperty("tcp_port")))
				.append(OsUtils.LINE_SEPARATOR)
				.append("Started container: " + name)
				.append(OsUtils.LINE_SEPARATOR)
				.append(OsUtils.LINE_SEPARATOR)
				.append("Documentation: https://github.com/SpringSource/spring-xd/wiki")
				.append(OsUtils.LINE_SEPARATOR);

			logger.info(sb.toString());
		}
	}

	@Override
	protected void onContainerStoppedEvent(ContainerStoppedEvent event) {

		String name = ManagementFactory.getRuntimeMXBean().getName();
		Container container = event.getSource();
		this.redisTemplate.boundHashOps("containers").delete(container.getId());
		this.redisTemplate.delete(container.getId());

		if (logger.isInfoEnabled()) {

			final String message = "Stopped container: " + name;
			final StringBuilder sb = new StringBuilder(OsUtils.LINE_SEPARATOR);
			sb.append(StringUtils.rightPad("", message.length(), "-"))
				.append(OsUtils.LINE_SEPARATOR)
				.append(message)
				.append(OsUtils.LINE_SEPARATOR)
				.append(StringUtils.rightPad("", message.length(), "-"))
				.append(OsUtils.LINE_SEPARATOR);

			logger.info(sb.toString());
		}

	}

}
