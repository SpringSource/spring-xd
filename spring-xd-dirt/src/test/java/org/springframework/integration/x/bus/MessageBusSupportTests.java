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

package org.springframework.integration.x.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.serializer.AbstractCodec;
import org.springframework.integration.x.bus.serializer.CompositeCodec;
import org.springframework.integration.x.bus.serializer.kryo.PojoCodec;
import org.springframework.integration.x.bus.serializer.kryo.TupleCodec;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * @author Gary Russell
 * @author David Turanski
 */
public class MessageBusSupportTests<P> {

	private final TestMessageBus messageBus = new TestMessageBus();

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {
		Map<Class<?>, AbstractCodec<Tuple>> codecs = new HashMap<Class<?>, AbstractCodec<Tuple>>();
		codecs.put(Tuple.class, new TupleCodec());
		messageBus.setCodec(new CompositeCodec(codecs, new PojoCodec()));
	}

	@Test
	public void testBytesPassThru() {
		byte[] payload = "foo".getBytes();
		Message<byte[]> message = MessageBuilder.withPayload(payload).build();
		Message<?> converted = messageBus.transformPayloadForProducerIfNecessary(message,
				APPLICATION_OCTET_STREAM);
		assertSame(payload, converted.getPayload());
		assertEquals(APPLICATION_OCTET_STREAM_VALUE,
				converted.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		Message<?> reconstructed = messageBus.transformPayloadForConsumerIfNecessary(converted,
				Collections.singletonList(ALL));
		payload = (byte[]) reconstructed.getPayload();
		assertSame(converted.getPayload(), payload);
		assertNull(reconstructed.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
	}

	@Test
	public void testBytesPassThruContentType() {
		byte[] payload = "foo".getBytes();
		Message<byte[]> message = MessageBuilder.withPayload(payload)
				.setHeader(MessageHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE)
				.build();
		Message<?> converted = messageBus.transformPayloadForProducerIfNecessary(message,
				APPLICATION_OCTET_STREAM);
		assertSame(payload, converted.getPayload());
		assertEquals(APPLICATION_OCTET_STREAM_VALUE,
				converted.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		Message<?> reconstructed = messageBus.transformPayloadForConsumerIfNecessary(converted,
				Collections.singletonList(ALL));
		payload = (byte[]) reconstructed.getPayload();
		assertSame(converted.getPayload(), payload);
		assertEquals(APPLICATION_OCTET_STREAM_VALUE,
				reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertNull(reconstructed.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
	}

	@Test
	public void testString() throws IOException {
		Message<?> converted = messageBus.transformPayloadForProducerIfNecessary(
				new GenericMessage<String>("foo"), APPLICATION_OCTET_STREAM);

		assertEquals(TEXT_PLAIN_VALUE,
				converted.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		Message<?> reconstructed = messageBus.transformPayloadForConsumerIfNecessary(converted,
				Collections.singletonList(ALL));
		assertEquals("foo", reconstructed.getPayload());
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testContentTypePreserved() throws IOException {
		Message<String> inbound = MessageBuilder.withPayload("{\"foo\":\"foo\"}")
				.copyHeaders(Collections.singletonMap(MessageHeaders.CONTENT_TYPE, APPLICATION_JSON))
				.build();
		Message<?> converted = messageBus.transformPayloadForProducerIfNecessary(
				inbound, APPLICATION_OCTET_STREAM);

		assertEquals(TEXT_PLAIN_VALUE,
				converted.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertEquals(APPLICATION_JSON,
				converted.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
		Message<?> reconstructed = messageBus.transformPayloadForConsumerIfNecessary(converted,
				Collections.singletonList(ALL));
		assertEquals("{\"foo\":\"foo\"}", reconstructed.getPayload());
		assertEquals(APPLICATION_JSON, new DefaultMessageMediaTypeResolver().resolveMediaType(reconstructed));
	}

	@Test
	public void testPojoSerialization() {
		Message<?> converted = messageBus.transformPayloadForProducerIfNecessary(
				new GenericMessage<Foo>(new Foo("bar")),
				APPLICATION_OCTET_STREAM);
		MediaType mediaType = new DefaultMessageMediaTypeResolver().resolveMediaType(converted);
		assertEquals("application", mediaType.getType());
		assertEquals("x-java-object", mediaType.getSubtype());
		assertEquals(Foo.class.getName(), mediaType.getParameter("type"));

		Message<?> reconstructed = messageBus.transformPayloadForConsumerIfNecessary(converted,
				Collections.singletonList(ALL));
		assertEquals("bar", ((Foo) reconstructed.getPayload()).getBar());
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testPojoWithXJavaObjectMediaTypeNoType() {
		Message<?> converted = messageBus.transformPayloadForProducerIfNecessary(
				new GenericMessage<Foo>(new Foo("bar")),
				APPLICATION_OCTET_STREAM);
		MediaType mediaType = new DefaultMessageMediaTypeResolver().resolveMediaType(converted);
		assertEquals("application", mediaType.getType());
		assertEquals("x-java-object", mediaType.getSubtype());
		assertEquals(Foo.class.getName(), mediaType.getParameter("type"));

		Message<?> reconstructed = messageBus.transformPayloadForConsumerIfNecessary(converted,
				Collections.singletonList(MediaType.valueOf("application/x-java-object")));
		assertEquals("bar", ((Foo) reconstructed.getPayload()).getBar());
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testPojoWithXJavaObjectMediaTypeExplicitType() {
		Message<?> converted = messageBus.transformPayloadForProducerIfNecessary(
				new GenericMessage<Foo>(new Foo("bar")),
				APPLICATION_OCTET_STREAM);
		MediaType mediaType = new DefaultMessageMediaTypeResolver().resolveMediaType(converted);
		assertEquals("application", mediaType.getType());
		assertEquals("x-java-object", mediaType.getSubtype());
		assertEquals(Foo.class.getName(), mediaType.getParameter("type"));

		Message<?> reconstructed = messageBus.transformPayloadForConsumerIfNecessary(converted,
				Collections.singletonList(mediaType));
		assertEquals("bar", ((Foo) reconstructed.getPayload()).getBar());
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testTupleSerialization() {
		Tuple payload = TupleBuilder.tuple().of("foo", "bar");
		Message<?> converted = messageBus.transformPayloadForProducerIfNecessary(new GenericMessage<Tuple>(payload),
				APPLICATION_OCTET_STREAM);
		MediaType mediaType = new DefaultMessageMediaTypeResolver().resolveMediaType(converted);
		assertEquals("application", mediaType.getType());
		assertEquals("x-java-object", mediaType.getSubtype());
		assertEquals(DefaultTuple.class.getName(), mediaType.getParameter("type"));

		Message<?> reconstructed = messageBus.transformPayloadForConsumerIfNecessary(converted,
				Collections.singletonList(ALL));
		assertEquals("bar", ((Tuple) reconstructed.getPayload()).getString("foo"));
		assertNull(reconstructed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	public static class Foo {

		private String bar;

		public Foo() {
		}

		public Foo(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

	}

	public static class Bar {

		private String foo;

		public Bar() {
		}

		public Bar(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	public class TestMessageBus extends MessageBusSupport {

		@Override
		public void bindConsumer(String name, MessageChannel channel, Collection<MediaType> acceptedMediaTypes,
				boolean aliasHint) {
		}

		@Override
		public void bindPubSubConsumer(String name, MessageChannel moduleInputChannel,
				Collection<MediaType> acceptedMediaTypes) {
		}

		@Override
		public void bindPubSubProducer(String name, MessageChannel moduleOutputChannel) {
		}

		@Override
		public void bindProducer(String name, MessageChannel channel, boolean aliasHint) {
		}
	}

}
