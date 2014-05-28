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

package org.springframework.xd.gemfire;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_STREAM_NAME;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.xd.module.options.spi.ModuleOption;
import org.springframework.xd.module.options.spi.ProfileNamesProvider;

/**
 * Describes options to the {@code gemfire} source module.
 * 
 * @author Eric Bottard
 * @author David Turanski
 */
public class GemfireSourceOptionsMetadata implements ProfileNamesProvider {

	private String cacheEventExpression = "newValue";

	private String regionName = XD_STREAM_NAME;

	private String host = "localhost";

	private int port = 40404;

	private boolean useLocator = false;

	public String getHost() {
		return host;
	}


	@ModuleOption("host name of the cache server or locator (if useLocator=true)")
	public void setHost(String host) {
		this.host = host;
	}


	public int getPort() {
		return port;
	}

	@ModuleOption("port of the cache server or locator (if useLocator=true)")
	public void setPort(int port) {
		this.port = port;
	}

	@NotBlank
	public String getCacheEventExpression() {
		return cacheEventExpression;
	}

	@ModuleOption("an optional SpEL expression referencing the event")
	public void setCacheEventExpression(String cacheEventExpression) {
		this.cacheEventExpression = cacheEventExpression;
	}

	@NotBlank
	public String getRegionName() {
		return regionName;
	}

	@ModuleOption("the name of the region for which events are to be monitored")
	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public boolean isUseLocator() {
		return useLocator;
	}

	@ModuleOption("set to true if using a locator")
	public void setUseLocator(boolean useLocator) {
		this.useLocator = useLocator;
	}

	@Override
	public String[] profilesToActivate() {
		if (useLocator) {
			return new String[] { "use-locator" };
		}
		else {
			return new String[] { "use-server" };
		}
	}
}
