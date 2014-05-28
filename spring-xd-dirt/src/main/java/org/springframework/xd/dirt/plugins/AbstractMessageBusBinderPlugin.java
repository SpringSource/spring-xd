/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ThreadUtils;

import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.Plugin;


/**
 * Abstract {@link Plugin} that has common implementation methods to bind/unbind {@link Module}'s message producers and
 * consumers to/from {@link MessageBus}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Turanski
 * @author Jennifer Hickey
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractMessageBusBinderPlugin extends AbstractPlugin {

	protected static final String MODULE_INPUT_CHANNEL = "input";

	protected static final String MODULE_OUTPUT_CHANNEL = "output";

	protected static final String TAP_CHANNEL_PREFIX = "tap:";

	protected static final String TOPIC_CHANNEL_PREFIX = "topic:";

	protected static final String JOB_CHANNEL_PREFIX = "job:";

	protected final MessageBus messageBus;

	/**
	 * Cache of children under the taps path.
	 */
	private volatile PathChildrenCache taps;

	/**
	 * A {@link PathChildrenCacheListener} implementation that monitors tap additions and removals.
	 */
	private final TapListener tapListener = new TapListener();

	/**
	 * Map of channels that can be tapped. The keys are the tap channel names (e.g. tap:stream:ticktock.time.0),
	 * and the values are the output channels from modules where the actual WireTap interceptors would be added. 
	 */
	private final Map<String, MessageChannel> tappableChannels = new HashMap<String, MessageChannel>();

	public AbstractMessageBusBinderPlugin(MessageBus messageBus) {
		this(messageBus, null);
	}

	public AbstractMessageBusBinderPlugin(MessageBus messageBus, ZooKeeperConnection zkConnection) {
		Assert.notNull(messageBus, "MessageBus must not be null.");
		this.messageBus = messageBus;
		if (zkConnection != null) {
			if (zkConnection.isConnected()) {
				startTapListener(zkConnection.getClient());
			}
			zkConnection.addListener(new TapLifecycleConnectionListener());
		}
	}

	private void startTapListener(CuratorFramework client) {
		String tapPath = Paths.build(Paths.TAPS);
		Paths.ensurePath(client, tapPath);
		taps = new PathChildrenCache(client, tapPath, true,
				ThreadUtils.newThreadFactory("TapsPathChildrenCache"));
		taps.getListenable().addListener(tapListener);
		try {
			taps.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e, "failed to start TapListener");
		}
	}

	/**
	 * Bind input/output channel of the module's message consumer/producers to {@link MessageBus}'s message
	 * source/target entities.
	 *
	 * @param module the module whose consumer and producers to bind to the {@link MessageBus}.
	 */
	protected final void bindConsumerAndProducers(Module module) {
		Properties[] properties = extractConsumerProducerProperties(module);
		MessageChannel inputChannel = module.getComponent(MODULE_INPUT_CHANNEL, MessageChannel.class);
		if (inputChannel != null) {
			bindMessageConsumer(inputChannel, getInputChannelName(module), properties[0]);
		}
		MessageChannel outputChannel = module.getComponent(MODULE_OUTPUT_CHANNEL, MessageChannel.class);
		if (outputChannel != null) {
			bindMessageProducer(outputChannel, getOutputChannelName(module), properties[1]);
			String tapChannelName = buildTapChannelName(module);
			tappableChannels.put(tapChannelName, outputChannel);
			if (isTapActive(tapChannelName)) {
				createAndBindTapChannel(tapChannelName, outputChannel);
			}
		}
	}

	protected final Properties[] extractConsumerProducerProperties(Module module) {
		Properties consumerProperties = new Properties();
		Properties producerProperties = new Properties();
		String consumerKeyPrefix = "consumer.";
		String producerKeyPrefix = "producer.";
		if (module.getDeploymentProperties() != null) {
			for (Map.Entry<String, String> entry : module.getDeploymentProperties().entrySet()) {
				if (entry.getKey().startsWith(consumerKeyPrefix)) {
					consumerProperties.put(entry.getKey().substring(consumerKeyPrefix.length()), entry.getValue());
				}
				else if (entry.getKey().startsWith(producerKeyPrefix)) {
					producerProperties.put(entry.getKey().substring(producerKeyPrefix.length()), entry.getValue());
				}
			}
		}
		return new Properties[] { consumerProperties, producerProperties };
	}

	protected abstract String getInputChannelName(Module module);

	protected abstract String getOutputChannelName(Module module);

	protected abstract String buildTapChannelName(Module module);

	private void bindMessageConsumer(MessageChannel inputChannel, String inputChannelName,
			Properties consumerProperties) {
		if (isChannelPubSub(inputChannelName)) {
			messageBus.bindPubSubConsumer(inputChannelName, inputChannel, consumerProperties);
		}
		else {
			messageBus.bindConsumer(inputChannelName, inputChannel, consumerProperties);
		}
	}

	private void bindMessageProducer(MessageChannel outputChannel, String outputChannelName,
			Properties producerProperties) {
		if (isChannelPubSub(outputChannelName)) {
			messageBus.bindPubSubProducer(outputChannelName, outputChannel, producerProperties);
		}
		else {
			messageBus.bindProducer(outputChannelName, outputChannel, producerProperties);
		}
	}

	/**
	 * Creates a wiretap on the output channel of the {@link Module} and binds the tap channel to {@link MessageBus}'s
	 * message target.
	 *
	 * @param tapChannelName the name of the tap channel
	 * @param outputChannel the channel to tap
	 */
	private void createAndBindTapChannel(String tapChannelName, MessageChannel outputChannel) {
		logger.info("creating and binding tap channel for %s", tapChannelName);
		if (outputChannel instanceof ChannelInterceptorAware) {
			MessageChannel tapChannel = tapOutputChannel(tapChannelName, (ChannelInterceptorAware) outputChannel);
			messageBus.bindPubSubProducer(tapChannelName, tapChannel, null); // TODO tap producer props
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("output channel is not interceptor aware. Tap will not be created.");
			}
		}
	}

	private MessageChannel tapOutputChannel(String tapChannelName, ChannelInterceptorAware outputChannel) {
		DirectChannel tapChannel = new DirectChannel();
		tapChannel.setBeanName(tapChannelName + ".tap.bridge");
		outputChannel.addInterceptor(new WireTap(tapChannel));
		return tapChannel;
	}

	/**
	 * Unbind input/output channel of the module's message consumer/producers from {@link MessageBus}'s message
	 * source/target entities.
	 *
	 * @param module the module whose consumer and producers to unbind from the {@link MessageBus}.
	 */
	protected final void unbindConsumerAndProducers(Module module) {
		MessageChannel inputChannel = module.getComponent(MODULE_INPUT_CHANNEL, MessageChannel.class);
		if (inputChannel != null) {
			messageBus.unbindConsumer(getInputChannelName(module), inputChannel);
		}
		MessageChannel outputChannel = module.getComponent(MODULE_OUTPUT_CHANNEL, MessageChannel.class);
		if (outputChannel != null) {
			messageBus.unbindProducer(getOutputChannelName(module), outputChannel);
			unbindTapChannel(buildTapChannelName(module));
		}
	}

	private void unbindTapChannel(String tapChannelName) {
		// Should this be unbindProducer() as there won't be multiple producers on the tap channel.
		MessageChannel tappedChannel = tappableChannels.remove(tapChannelName);
		if (tappedChannel instanceof ChannelInterceptorAware) {
			ChannelInterceptorAware interceptorAware = ((ChannelInterceptorAware) tappedChannel);
			List<ChannelInterceptor> interceptors = new ArrayList<ChannelInterceptor>();
			for (ChannelInterceptor interceptor : interceptorAware.getChannelInterceptors()) {
				if (interceptor instanceof WireTap) {
					((WireTap) interceptor).stop();
				}
				else {
					interceptors.add(interceptor);
				}
			}
			interceptorAware.setInterceptors(interceptors);
		}
		messageBus.unbindProducers(tapChannelName);
	}

	private boolean isChannelPubSub(String channelName) {
		Assert.isTrue(StringUtils.hasText(channelName), "Channel name should not be empty/null.");
		// Check if the channelName starts with tap: or topic:
		return (channelName.startsWith(TAP_CHANNEL_PREFIX) || channelName.startsWith(TOPIC_CHANNEL_PREFIX));
	}

	@Override
	public int getOrder() {
		return 0;
	}


	/**
	 * Event handler for tap additions.
	 *
	 * @param client curator client
	 * @param data module data
	 */
	private void onTapAdded(CuratorFramework client, ChildData data) {
		String tapChannelName = buildTapChannelNameFromPath(data.getPath());
		MessageChannel outputChannel = tappableChannels.get(tapChannelName);
		if (outputChannel != null) {
			createAndBindTapChannel(tapChannelName, outputChannel);
		}
	}

	/**
	 * Event handler for tap removals.
	 *
	 * @param client curator client
	 * @param data module data
	 */
	private void onTapRemoved(CuratorFramework client, ChildData data) {
		unbindTapChannel(buildTapChannelNameFromPath(data.getPath()));
	}

	/**
	 * Checks whether the provided tap channel name has one or more active subscribers.
	 *
	 * @param tapChannelName the tap channel to check
	 *
	 * @return {@code true} if the tap does have one or more active subscribers
	 */
	private boolean isTapActive(String tapChannelName) {
		Assert.state(taps != null, "tap cache not started");
		List<ChildData> currentTaps = taps.getCurrentData();
		for (ChildData data : currentTaps) {
			// example path: /taps/stream:ticktock.time.0
			if (buildTapChannelNameFromPath(data.getPath()).equals(tapChannelName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generates the name of a tap channel given a ZooKeeper tap path.
	 *
	 * @param path the ZooKeeper path under {@link Paths#TAPS}.
	 * 
	 * @return the tap channel name
	 */
	private String buildTapChannelNameFromPath(String path) {
		return TAP_CHANNEL_PREFIX + Paths.stripPath(path);
	}


	/**
	 * Listener for tap additions and removals under {@link Paths#TAPS}.
	 */
	class TapListener implements PathChildrenCacheListener {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
			ZooKeeperUtils.logCacheEvent(logger, event);
			switch (event.getType()) {
				case INITIALIZED:
					break;
				case CHILD_ADDED:
					onTapAdded(client, event.getData());
					break;
				case CHILD_REMOVED:
					onTapRemoved(client, event.getData());
					break;
				default:
					break;
			}
		}
	}


	/**
	 * A {@link ZooKeeperConnectionListener} that manages the lifecycle of the taps cache listener.
	 */
	class TapLifecycleConnectionListener implements ZooKeeperConnectionListener {

		@Override
		public void onDisconnect(CuratorFramework client) {
			taps.getListenable().removeListener(tapListener);
			taps.clear();
		}

		@Override
		public void onConnect(CuratorFramework client) {
			startTapListener(client);
		}
	}

}
