/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.module;


import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.xd.module.ModuleType.processor;
import static org.springframework.xd.module.ModuleType.sink;
import static org.springframework.xd.module.ModuleType.source;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;


/**
 * Tests for DelegatingModuleRegistry.
 * 
 * @author Eric Bottard
 * @author Glenn Renfro
 */
public class DelegatingModuleRegistryTests {

	private ModuleRegistry registry;

	@Before
	public void setup() {
		String thisClass = DelegatingModuleRegistryTests.class.getName().replace(".", "/");

		ResourceModuleRegistry cp = new ResourceModuleRegistry(
				String.format("classpath:/%s/classpath-based", thisClass));
		ResourceModuleRegistry file = new ResourceModuleRegistry(String.format(
				"file:src/test/resources/%s/file-based", thisClass));
		registry = new DelegatingModuleRegistry(cp, file);
	}

	/**
	 * Tests that when at least one delegate has a module, it is found.
	 */
	@Test
	public void testFound() {
		ModuleDefinition def = registry.findDefinition("test-source", source);
		assertThat(def, notNullValue());
		assertThat(def, module("test-source", source));
	}

	/**
	 * Tests that when no delegate has a module, it is not found.
	 */
	@Test
	public void testNotFound() {
		ModuleDefinition def = registry.findDefinition("foo", sink);
		assertNull(def);
	}


	/**
	 * Tests the find [all] definitions behavior.
	 */
	@Test
	public void testFindDefinitions() {
		List<ModuleDefinition> definitions = registry.findDefinitions();

		assertThat(definitions, containsInAnyOrder(module("test-source", source),
				module("test-processor", processor),
				module("test-sink", sink),
				module("foobar", source),
				module("foobar", sink)
				));

	}

	private Matcher<ModuleDefinition> module(final String name, final ModuleType type) {
		return new DiagnosingMatcher<ModuleDefinition>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("a module named ").appendValue(name).appendText(" with type ").appendValue(type);
			}

			@Override
			protected boolean matches(Object item, Description mismatchDescription) {
				ModuleDefinition def = (ModuleDefinition) item;
				mismatchDescription.appendText("a module named ").appendValue(def.getName())
						.appendText(" with type ").appendValue(def.getType());
				return name.equals(def.getName()) && type == def.getType();
			}
		};
	}

	/**
	 * Tests that when several delegates define the same module, the first one wins.
	 */
	@Test
	public void testShadowing() {
		ModuleDefinition def = registry.findDefinition("test-processor", processor);
		assertThat(def.getResource(), instanceOf(ClassPathResource.class));
	}

	@Test
	public void testFindByName() {
		List<ModuleDefinition> result = registry.findDefinitions("foobar");
		assertThat(result, containsInAnyOrder(module("foobar", source), module("foobar", sink)));
	}

	@Test
	public void testFindByType() {
		List<ModuleDefinition> result = registry.findDefinitions(source);
		assertThat(result, containsInAnyOrder(module("test-source", source), module("foobar", source)));
	}
}
