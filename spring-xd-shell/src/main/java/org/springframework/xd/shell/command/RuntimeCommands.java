/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.shell.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.xd.rest.client.RuntimeOperations;
import org.springframework.xd.rest.domain.ContainerResource;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.shell.XDShell;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableHeader;
import org.springframework.xd.shell.util.TableRow;

/**
 * Commands to interact with runtime containers/modules.
 * 
 * @author Ilayaperumal Gopinathan
 */
@Component
public class RuntimeCommands implements CommandMarker {

	private static final String LIST_CONTAINERS = "runtime containers";

	private static final String LIST_MODULES = "runtime modules";

	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ LIST_CONTAINERS, LIST_MODULES })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = LIST_CONTAINERS, help = "List runtime containers")
	public Table listContainers() {
		final PagedResources<ContainerResource> containers = runtimeOperations().listRuntimeContainers();
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Container Id"))
				.addHeader(2, new TableHeader("Host"))
				.addHeader(3, new TableHeader("IP Address"))
				.addHeader(4, new TableHeader("PID"))
				.addHeader(5, new TableHeader("Groups"))
				.addHeader(6, new TableHeader("Custom Attributes"));
		for (ContainerResource container : containers) {
			Map<String, String> copy = new HashMap<String, String>(container.getAttributes());
			final TableRow row = table.newRow();
			row.addValue(1, copy.remove("id"))
					.addValue(2, copy.remove("host"))
					.addValue(3, copy.remove("ip"))
					.addValue(4, copy.remove("pid"));
			String groups = copy.remove("groups");
			row.addValue(5, groups == null ? "" : groups);
			row.addValue(6, copy.isEmpty() ? "" : copy.toString());
		}
		return table;
	}

	@CliCommand(value = LIST_MODULES, help = "List runtime modules")
	public Table listDeployedModules(
			@CliOption(mandatory = false, key = { "containerId" }, help = "to filter by container id") String containerId,
			@CliOption(mandatory = false, key = { "moduleId" }, help = "to filter by module id") String moduleId) {
		Iterable<ModuleMetadataResource> runtimeModules;
		if (StringUtils.hasText(containerId) && StringUtils.hasText(moduleId)) {
			runtimeModules = Collections.singletonList(runtimeOperations().listRuntimeModule(containerId, moduleId));
		}
		else if (StringUtils.hasText(containerId)) {
			runtimeModules = runtimeOperations().listRuntimeModulesByContainer(containerId);
		}
		else if (StringUtils.hasText(moduleId)) {
			runtimeModules = runtimeOperations().listRuntimeModulesByModuleId(moduleId);
		}
		else {
			runtimeModules = runtimeOperations().listRuntimeModules();
		}
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Unit Name")).addHeader(2, new TableHeader("Type")).addHeader(3,
				new TableHeader("Module")).addHeader(4, new TableHeader("Container Id")).addHeader(5,
				new TableHeader("Options")).addHeader(6, new TableHeader("Deployment Properties")).addHeader(7,
				new TableHeader("Unit status"));
		for (ModuleMetadataResource module : runtimeModules) {
			final TableRow row = table.newRow();
			row.addValue(1, module.getUnitName()).addValue(2, module.getModuleType()).addValue(3, module.getName()).addValue(
					4, module.getContainerId()).addValue(5, module.getModuleOptions().toString()).addValue(6,
					module.getDeploymentProperties().toString()).addValue(7, module.getDeploymentStatus());
		}
		return table;
	}

	private RuntimeOperations runtimeOperations() {
		return xdShell.getSpringXDOperations().runtimeOperations();
	}

}
