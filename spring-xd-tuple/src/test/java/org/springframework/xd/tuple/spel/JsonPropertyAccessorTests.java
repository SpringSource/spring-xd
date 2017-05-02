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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Tests for {@link JsonPropertyAccessor}.
 * 
 * @author Eric Bottard
 */
public class JsonPropertyAccessorTests {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final StandardEvaluationContext context = new StandardEvaluationContext();

	private final ObjectMapper mapper = new ObjectMapper();

	@Before
	public void setup() {
		context.addPropertyAccessor(new JsonPropertyAccessor());
	}

	@Test
	public void testSimpleLookup() throws Exception {
		Object json = mapper.readTree("{\"foo\": \"bar\"}");
		JsonNode actual = evaluate(json, "foo", JsonNode.class);
		assertEquals("bar", actual.asText());
	}

	@Test(expected = SpelEvaluationException.class)
	public void testUnsupportedJsonConstruct() throws Exception {
		Object json = mapper.readTree("\"foo\"");
		evaluate(json, "fizz", JsonNode.class);
	}

	@Test(expected = SpelEvaluationException.class)
	public void testMissingProperty() throws Exception {
		Object json = mapper.readTree("{\"foo\": \"bar\"}");
		evaluate(json, "fizz", JsonNode.class);
	}

	@Test
	public void testArrayLookup() throws Exception {
		Object json = mapper.readTree("[3, 4, 5]");
		Map<String, Object> root = Collections.singletonMap("foo", json);
		// JsonNode actual = evaluate("1", json, JsonNode.class); // Does not work
		// JsonNode actual = evaluate("'1'", json, JsonNode.class); // Does not work
		JsonNode actual = evaluate(json, "['1']", JsonNode.class);
		assertEquals(4, actual.asInt());
	}

	@Test
	public void testNestedArrayConstruct() throws Exception {
		Object json = mapper.readTree("[[3], [4, 5], []]");
		// JsonNode actual = evaluate("1.1", json, JsonNode.class); // Does not work
		// JsonNode actual = evaluate("[1][1]", json, JsonNode.class); // Does not work
		JsonNode actual = evaluate(json, "['1']['1']", JsonNode.class);
		assertEquals(5, actual.asInt());
	}


	@Test
	public void testNestedHashConstruct() throws Exception {
		Object json = mapper.readTree("{\"foo\": {\"bar\": 4, \"fizz\": 5} }");
		JsonNode actual = evaluate(json, "foo.fizz", JsonNode.class);
		assertEquals(5, actual.asInt());
	}

	@Test
	public void testWriteInHash() throws Exception {
		JsonNode json = mapper.readTree("{\"foo\": 4}");

		assign(json, "bar", 42L);
		assertTrue(json.get("bar").isLong());
		assertEquals(42L, json.get("bar").longValue());

		assign(json, "foo", "fizz");
		assertEquals("fizz", json.get("foo").asText());
	}

	@Test
	public void testWriteInArray() throws Exception {
		JsonNode json = mapper.readTree("[4, 5, 6]");

		assign(json, "['1']", 42);
		assertEquals(42, json.get(1).asInt());
	}

	@Test
	public void testWriteNested() throws Exception {
		JsonNode json = mapper.readTree("{\"foo\": 42}");
		Integer[] value = new Integer[] { 3, 4, 5 };

		assign(json, "foo", value);
		assertEquals(5, json.get("foo").get(2).asInt());
	}

	private <T> T evaluate(Object target, String expression, Class<T> expectedType) {
		return parser.parseExpression(expression).getValue(context, target, expectedType);
	}

	private void assign(Object target, String expression, Object value) {
		parser.parseExpression(expression).setValue(context, target, value);
	}
}
