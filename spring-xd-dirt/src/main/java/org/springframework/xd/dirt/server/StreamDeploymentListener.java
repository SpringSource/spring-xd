/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.ContainerRepository;
import org.springframework.xd.dirt.core.ModuleDeploymentProperties;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDescriptor;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Listener implementation that handles stream deployment requests.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class StreamDeploymentListener implements PathChildrenCacheListener {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(StreamDeploymentListener.class);

	/**
	 * Utility to convert maps to byte arrays.
	 */
	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * Stream factory.
	 */
	private final StreamFactory streamFactory;

	/**
	 * Utility for writing module deployment requests to ZooKeeper.
	 */
	private final ModuleDeploymentWriter moduleDeploymentWriter;

	/**
	 * Executor service dedicated to handling events raised from
	 * {@link org.apache.curator.framework.recipes.cache.PathChildrenCache}.
	 *
	 * @see #childEvent
	 * @see StreamDeploymentListener.EventHandler
	 */
	private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "Stream Deployer");
			thread.setDaemon(true);
			return thread;
		}
	});

	/**
	 * Construct a StreamDeploymentListener.
	 *
	 * @param containerRepository repository to obtain container data
	 * @param moduleDefinitionRepository repository to obtain module data
	 * @param moduleOptionsMetadataResolver resolver for module options metadata
	 * @param containerMatcher matches modules to containers
	 */
	public StreamDeploymentListener(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamDefinitionRepository streamDefinitionRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver,
			ContainerMatcher containerMatcher) {
		this.streamFactory = new StreamFactory(streamDefinitionRepository,
				moduleDefinitionRepository, moduleOptionsMetadataResolver);
		this.moduleDeploymentWriter = new ModuleDeploymentWriter(zkConnection,
				containerRepository, containerMatcher);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Handle child events for the {@link Paths#STREAMS} path.
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		ZooKeeperConnection.logCacheEvent(logger, event);
		executorService.submit(new EventHandler(client, event));
	}

	/**
	 * Handle the creation of a new stream deployment.
	 *
	 * @param client curator client
	 * @param data stream deployment request data
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		String streamName = Paths.stripPath(data.getPath());

		byte[] streamDefinition = client.getData().forPath(Paths.build(Paths.STREAMS, streamName));
		Map<String, String> map = mapBytesUtility.toMap(streamDefinition);

		byte[] deploymentPropertiesData = data.getData();
		if (deploymentPropertiesData != null && deploymentPropertiesData.length > 0) {
			map.put("deploymentProperties", new String(deploymentPropertiesData, "UTF-8"));
		}
		Stream stream = streamFactory.createStream(streamName, map);

		logger.info("Deploying stream {} with properties {}", stream, map);
		prepareStream(client, stream);
		deployStream(stream);
		logger.info("Stream {} deployment attempt complete", stream);
	}

	/**
	 * Prepare the new stream for deployment. This updates the ZooKeeper znode for the stream by adding the following
	 * under {@code /xd/streams/[stream-name]}:
	 * <ul>
	 * <li>{@code .../source/[module-name.module-label]}</li>
	 * <li>{@code .../processor/[module-name.module-label]}</li>
	 * <li>{@code .../sink/[module-name.module-label]}</li>
	 * </ul>
	 * The children of these nodes will be ephemeral nodes written by the containers that accept deployment of the
	 * modules.
	 *
	 * @param client curator client
	 * @param stream stream to be prepared
	 */
	private void prepareStream(CuratorFramework client, Stream stream) throws Exception {
		for (Iterator<ModuleDescriptor> iterator = stream.getDeploymentOrderIterator(); iterator.hasNext();) {
			ModuleDescriptor descriptor = iterator.next();
			String streamName = stream.getName();
			String moduleType = descriptor.getModuleDefinition().getType().toString();
			String moduleLabel = descriptor.getModuleLabel();

			String path = new StreamDeploymentsPath()
					.setStreamName(streamName)
					.setModuleType(moduleType)
					.setModuleLabel(moduleLabel).build();

			try {
				client.create().creatingParentsIfNeeded().forPath(path);
			}
			catch (KeeperException.NodeExistsException e) {
				// todo: this would be somewhat unexpected
				logger.info("Path {} already exists", path);
			}
		}
	}

	/**
	 * Issue deployment requests for the modules of the given stream.
	 *
	 * @param stream stream to be deployed
	 *
	 * @throws Exception
	 */
	private void deployStream(final Stream stream) throws Exception {
		ModuleDeploymentWriter.ModuleDeploymentPropertiesProvider provider =
				new ModuleDeploymentWriter.ModuleDeploymentPropertiesProvider() {
			@Override
			public ModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor descriptor) {
				return createModuleDeploymentProperties(stream.getDeploymentProperties(), descriptor);
			}
		};

		Collection<ModuleDeploymentWriter.Result> results =
				moduleDeploymentWriter.writeDeployment(stream.getDeploymentOrderIterator(), provider);
		moduleDeploymentWriter.validateResults(results);
	}

	/**
	 * Based on the deployment properties for a {@link Stream}, create an instance
	 * of {@link org.springframework.xd.dirt.core.ModuleDeploymentProperties} for
	 * a specific module in the stream.
	 *
	 * @param streamDeploymentProperties deployment properties for a stream
	 * @param descriptor descriptor for module in the stream for which
	 *        to create the properties
	 * @return deployment properties for the module
	 */
	public static ModuleDeploymentProperties createModuleDeploymentProperties(
			Map<String, String> streamDeploymentProperties, ModuleDescriptor descriptor) {
		ModuleDeploymentProperties deploymentProperties = new ModuleDeploymentProperties();
		for (String key : streamDeploymentProperties.keySet()) {
			String prefix = String.format("module.%s.", descriptor.getModuleName());
			if (key.startsWith(prefix)) {
				deploymentProperties.put(key.substring(prefix.length()),
						streamDeploymentProperties.get(key));
			}
		}
		return deploymentProperties;
	}

	/**
	 * Callable that handles events from a {@link org.apache.curator.framework.recipes.cache.PathChildrenCache}. This
	 * allows for the handling of events to be executed in a separate thread from the Curator thread that raises these
	 * events.
	 */
	class EventHandler implements Callable<Void> {

		/**
		 * Curator client.
		 */
		private final CuratorFramework client;

		/**
		 * Event raised from Curator.
		 */
		private final PathChildrenCacheEvent event;

		/**
		 * Construct an {@code EventHandler}.
		 *
		 * @param client curator client
		 * @param event event raised from Curator
		 */
		EventHandler(CuratorFramework client, PathChildrenCacheEvent event) {
			this.client = client;
			this.event = event;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Void call() throws Exception {
			try {
				switch (event.getType()) {
					case CHILD_ADDED:
						onChildAdded(client, event.getData());
						break;
					default:
						break;
				}
				return null;
			}
			catch (Exception e) {
				logger.error("Exception caught while handling event", e);
				throw e;
			}
		}
	}

}
