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

package org.springframework.xd.tuple.spel;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import com.fasterxml.jackson.databind.node.ContainerNode;


/**
 * A SpEL {@link PropertyAccessor} that know how to read and write on Jackson JSON objects.
 * 
 * @author Eric Bottard
 */
public class JsonPropertyAccessor implements PropertyAccessor {

	/**
	 * The kind of types this can work with.
	 */
	private static final Class[] SUPPORTED_CLASSES = new Class[] { ContainerNode.class };

	@Override
	public Class[] getSpecificTargetClasses() {
		return SUPPORTED_CLASSES;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		ContainerNode<?> container = (ContainerNode<?>) target;
		Integer index = maybeIndex(name);
		return (index != null && container.has(index) || container.has(name));
	}

	/**
	 * Return an integer if the String property name can be parsed as an int, or null otherwise.
	 */
	private Integer maybeIndex(String name) {
		try {
			return Integer.valueOf(name);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		ContainerNode<?> container = (ContainerNode<?>) target;
		Integer index = maybeIndex(name);
		if (index != null && container.has(index)) {
			return new TypedValue(container.get(index));
		}
		else {
			return new TypedValue(container.get(name));
		}
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
