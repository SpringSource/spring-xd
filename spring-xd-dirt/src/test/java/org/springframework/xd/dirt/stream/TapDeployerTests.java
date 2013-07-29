/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.xd.dirt.stream.memory.InMemoryStreamDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryTapDefinitionRepository;
import org.springframework.xd.dirt.stream.memory.InMemoryTapInstanceRepository;

/**
 * @author David Turanski
 * @author Gunnar Hillert
 *
 */
public class TapDeployerTests {

	private TapDefinitionRepository repository;

	private TapInstanceRepository tapInstanceRepository;

	private StreamDefinitionRepository streamRepository;

	private SubscribableChannel outputChannel;

	private DeploymentMessageSender sender;

	private TapDeployer tapDeployer;

	@Before
	public void setUp() {
		repository = new InMemoryTapDefinitionRepository();
		tapInstanceRepository = new InMemoryTapInstanceRepository();
		streamRepository = new InMemoryStreamDefinitionRepository();
		outputChannel = new DirectChannel();
		sender = new DeploymentMessageSender(outputChannel);
		tapDeployer = new TapDeployer(repository, streamRepository, sender, tapInstanceRepository);
	}

	@Test
	public void testCreateSucceeds() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		streamRepository.save(new StreamDefinition("test", "time | log"));
		tapDeployer.save(tapDefinition);
		assertTrue(repository.exists("tap1"));
	}

	@Test(expected = NoSuchDefinitionException.class)
	public void testCreateFailsIfSourceStreamDoesNotExist() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		tapDeployer.save(tapDefinition);
	}

	@Test
	public void testDeploySucceeds() {
		TapDefinition tapDefinition = new TapDefinition("tap1", "test", "tap @test | file");
		repository.save(tapDefinition);
		final AtomicInteger messageCount = new AtomicInteger();
		outputChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageCount.getAndIncrement();
			}
		});

		tapDeployer.deploy("tap1");
		assertEquals(2, messageCount.get());
	}

	@Test(expected = NoSuchDefinitionException.class)
	public void testDeployFailsForMissingDefinition() {
		tapDeployer.deploy("tap1");
	}

	@After
	public void clearRepos() {
		repository.deleteAll();
		streamRepository.deleteAll();
	}
}
