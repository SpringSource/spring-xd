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

package org.springframework.xd.shell.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.xd.shell.XDShell;

/**
 * Tap commands.
 *
 * @author Ilayaperumal Gopinathan
 */

@Component
public class TapCommands implements CommandMarker {

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ "create tap" })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = "create tap", help = "Create a tap")
	public String createTap(
			@CliOption(mandatory = true, key = "name", help = "the name to give to the tap")
			String name,
			@CliOption(mandatory = true, key = { "", "definition" }, help = "Tap definition, using XD DSL (e.g. \"tap@mystream.filter | sink1\")")
			String dsl,
			@CliOption(key = "autostart", help = "flag to set true to autostart after creation")
			Boolean autoStart) {
		try {
			xdShell.getSpringXDOperations().createTap(name, dsl, autoStart);
		} 
		catch (Exception e) {
			return String.format("Error creating tap '%s'", name);
		}
		return String.format(((autoStart != null && autoStart.booleanValue()) ? 
				"Successfully created and deployed tap '%s'" : "Successfully created tap '%s'"), name);
	}

}
