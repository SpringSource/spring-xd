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

package org.springframework.integration.x.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;


/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Jennifer Hickey
 */
public class NettyHttpInboundChannelAdapterTests {

	@Test
	public void test() throws Exception {
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(2);
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messages.add(message);
				latch.countDown();
			}
		});
		int port = SocketUtils.findAvailableServerSocket();
		NettyHttpInboundChannelAdapter adapter = new NettyHttpInboundChannelAdapter(port);
		adapter.setOutputChannel(channel);
		adapter.start();
		RestTemplate template = new RestTemplate();
		URI uri1 = new URI("http://localhost:" + port + "/test1");
		URI uri2 = new URI("http://localhost:" + port + "/test2");
		ResponseEntity<?> response1 = template.postForEntity(uri1, "foo", Object.class);
		ResponseEntity<?> response2 = template.postForEntity(uri2, "bar", Object.class);
		assertEquals(HttpStatus.OK, response1.getStatusCode());
		assertEquals(HttpStatus.OK, response2.getStatusCode());
		assertTrue(latch.await(1, TimeUnit.SECONDS));
		assertEquals(2, messages.size());
		Message<?> message1 = messages.get(0);
		Message<?> message2 = messages.get(1);
		assertEquals("foo", message1.getPayload());
		assertEquals("bar", message2.getPayload());
		assertEquals("/test1", message1.getHeaders().get("requestPath"));
		assertEquals("/test2", message2.getHeaders().get("requestPath"));
	}

	@Test
	public void testContentTypeHeaderMapsToSiContentTypeHeader() throws Exception {
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final CountDownLatch latch = new CountDownLatch(1);
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messages.add(message);
				latch.countDown();
			}
		});
		int port = SocketUtils.findAvailableServerSocket();
		NettyHttpInboundChannelAdapter adapter = new NettyHttpInboundChannelAdapter(port);
		adapter.setOutputChannel(channel);
		adapter.start();
		RestTemplate template = new RestTemplate();
		URI uri1 = new URI("http://localhost:" + port + "/test1");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		HttpEntity<String> entity = new HttpEntity<String>("foo", headers);

		ResponseEntity<?> response = template.postForEntity(uri1, entity, HttpEntity.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());

		assertTrue(latch.await(1, TimeUnit.SECONDS));
		assertEquals(1, messages.size());
		Message<?> message = messages.get(0);

		assertEquals(MediaType.TEXT_PLAIN_VALUE, message.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test(expected = HttpServerErrorException.class)
	public void testErrorResponse() throws URISyntaxException {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException();
			}
		});
		int port = SocketUtils.findAvailableServerSocket();
		NettyHttpInboundChannelAdapter adapter = new NettyHttpInboundChannelAdapter(port);
		adapter.setOutputChannel(channel);
		adapter.start();
		RestTemplate template = new RestTemplate();
		URI uri1 = new URI("http://localhost:" + port + "/test1");
		template.postForEntity(uri1, "foo", Object.class);
	}

	@Test
	public void testCustomExecutor() throws Exception {
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		final Set<String> threadNames = new HashSet<String>();
		final CountDownLatch latch = new CountDownLatch(1);
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				threadNames.add(Thread.currentThread().getName());
				messages.add(message);
				latch.countDown();
			}
		});
		int port = SocketUtils.findAvailableServerSocket();
		NettyHttpInboundChannelAdapter adapter = new NettyHttpInboundChannelAdapter(port);
		adapter.setOutputChannel(channel);
		adapter.setExecutor(Executors.newFixedThreadPool(1, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "executor-test");
			}
		}));
		adapter.start();
		RestTemplate template = new RestTemplate();
		URI uri1 = new URI("http://localhost:" + port + "/test1");
		URI uri2 = new URI("http://localhost:" + port + "/test2");
		ResponseEntity<?> response1 = template.postForEntity(uri1, "foo", Object.class);
		ResponseEntity<?> response2 = template.postForEntity(uri2, "bar", Object.class);
		assertEquals(HttpStatus.OK, response1.getStatusCode());
		assertEquals(HttpStatus.OK, response2.getStatusCode());
		assertTrue(latch.await(1, TimeUnit.SECONDS));
		// Ensure messages were received on the single thread with custom name
		assertEquals(Collections.singleton("executor-test"), threadNames);
		assertEquals(2, messages.size());
		Message<?> message1 = messages.get(0);
		Message<?> message2 = messages.get(1);
		assertEquals("foo", message1.getPayload());
		assertEquals("bar", message2.getPayload());
		assertEquals("/test1", message1.getHeaders().get("requestPath"));
		assertEquals("/test2", message2.getHeaders().get("requestPath"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullExecutor() {
		int port = SocketUtils.findAvailableServerSocket();
		NettyHttpInboundChannelAdapter adapter = new NettyHttpInboundChannelAdapter(port);
		adapter.setExecutor(null);
	}
}
