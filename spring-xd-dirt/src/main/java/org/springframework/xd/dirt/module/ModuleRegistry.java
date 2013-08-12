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

package org.springframework.xd.dirt.module;

import java.util.List;

import org.springframework.xd.module.ModuleDefinition;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Glenn Renfro
 */
public interface ModuleRegistry {

	ModuleDefinition lookup(String name, String type);

	/**
	 * Searches the registry for the name specified and returns all module definitions
	 * that match the name regardless of module type.
	 * 
	 * @param name The module definition name to be searched.
	 * @return A list of the module definitions that have the name specified by the input
	 *         parameter. If no module definition is found with the name a null will be
	 *         returned.
	 */
	List<ModuleDefinition> findDefinitions(String name);

}
