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

package org.springframework.xd.dirt.module.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.MapUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.util.PagingUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * ZooKeeper backed repository for runtime info about deployed modules.
 * 
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 */
public class ZooKeeperModuleMetadataRepository implements ModuleMetadataRepository {

	private final ZooKeeperConnection zkConnection;

	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	private final PagingUtility<ModuleMetadata> pagingUtility = new PagingUtility<ModuleMetadata>();

	private static final String XD_MODULE_PROPERTIES_PREFIX = "xd.";

	@Autowired
	public ZooKeeperModuleMetadataRepository(ZooKeeperConnection zkConnection) {
		this.zkConnection = zkConnection;
	}

	@Override
	public Iterable<ModuleMetadata> findAll(Sort sort) {
		// todo: add support for sort
		return findAll();
	}

	@Override
	public Page<ModuleMetadata> findAll(Pageable pageable) {
		return pagingUtility.getPagedData(pageable, findAll());
	}

	@Override
	public <S extends ModuleMetadata> Iterable<S> save(Iterable<S> entities) {
		List<S> results = new ArrayList<S>();
		for (S entity : entities) {
			results.add(save(entity));
		}
		return results;
	}

	/**
	 * Find the module metadata for the modules that are deployed into the
	 * given container and module id.
	 *
	 * @param containerId the containerId of the container
	 * @param moduleId the module id to use.
	 *
	 * @return {@link ModuleMetadata} of the module.
	 */
	@Override
	public ModuleMetadata findOne(String containerId, String moduleId) {
		Assert.hasLength(containerId, "containerId is required");
		Assert.hasLength(moduleId, "moduleId is required");
		ModuleMetadata md = findOne(metadataPath(containerId, moduleId));
		return md;
	}

	@Override
	public ModuleMetadata findOne(String metadataPath) {
		ModuleMetadata metadata = null;
		try {
			byte[] data = zkConnection.getClient().getData().forPath(metadataPath);
			if (data != null) {
				Map<String, String> metadataMap = mapBytesUtility.toMap(data);
				String moduleId = getModuleId(metadataPath);
				String containerId = getContainerId(metadataPath);
				metadata = new ModuleMetadata(moduleId, containerId, getResolvedModuleOptions(metadataMap),
						getDeploymentProperties(metadataPath));
			}
		}
		catch (Exception e) {
			// NoNodeException - this node does not exist, will return null
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return metadata;
	}

	/**
	 * Get the deployment properties associated with this module metadata path.
	 *
	 * @param metadataPath the module metadata path
	 * @return the deployment
	 */
	private Properties getDeploymentProperties(String metadataPath) {
		Map<String, String> deploymentProperties = new HashMap<String, String>();
		try {
			// Get the deployment properties from module deployment path
			String moduleDeploymentsPath = metadataPath.substring(0, metadataPath.lastIndexOf("/"));
			deploymentProperties = mapBytesUtility.toMap(zkConnection.getClient().getData().forPath(
					moduleDeploymentsPath));
		}
		catch (Exception e) {
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return MapUtils.toProperties(deploymentProperties);
	}

	/**
	 * Resolve the module option value using the module metadata.
	 *
	 * @param metadataMap the values map from ZK module metadata
	 * @return the resolved option values
	 */
	private Properties getResolvedModuleOptions(Map<String, String> metadataMap) {
		Map<String, String> optionsMap = new HashMap<String, String>();
		for (String propertyKey : metadataMap.keySet()) {
			String propertyValue = metadataMap.get(propertyKey);
			if (!propertyKey.startsWith(XD_MODULE_PROPERTIES_PREFIX)
					&& !StringUtils.isEmpty(propertyValue)) {
				if (propertyValue.startsWith(PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX) &&
						propertyValue.endsWith(PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX)) {
					// For a property ${module.property}, we just extract "module.property" and
					// check if the metadataMap has a value for it.
					String placeholderKey = propertyValue.substring(2, propertyValue.length() - 1);
					if (metadataMap.get(placeholderKey) != null) {
						propertyValue = metadataMap.get(placeholderKey);
					}
				}
				optionsMap.put(propertyKey, propertyValue);
			}
		}
		return MapUtils.toProperties(optionsMap);
	}

	private String getModuleId(String metadataPath) {
		String modulePath = metadataPath.substring(0, metadataPath.lastIndexOf("/"));
		return Paths.stripPath(modulePath);
	}

	private String getContainerId(String metadataPath) {
		String modulePath = metadataPath.substring(0, metadataPath.lastIndexOf("/"));
		String containerPath = modulePath.substring(0, modulePath.lastIndexOf("/"));
		return Paths.stripPath(containerPath);
	}

	@Override
	public boolean exists(String id) {
		try {
			return null != zkConnection.getClient().checkExists()
					.forPath(path(id));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public List<ModuleMetadata> findAll() {
		List<ModuleMetadata> results = new ArrayList<ModuleMetadata>();
		try {
			for (String containerId : getAvailableContainerIds()) {
				List<String> modules = getDeployedModules(containerId);
				for (String moduleId : modules) {
					ModuleMetadata metadata = findOne(containerId, moduleId);
					if (metadata != null) {
						results.add(metadata);
					}
				}
			}
			return results;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * Get all the available containers' ids.
	 *
	 * @return the list of all available containers' ids.
	 */
	private List<String> getAvailableContainerIds() {
		try {
			return zkConnection.getClient().getChildren().forPath(Paths.CONTAINERS);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * Find the module metadata for the modules that are deployed into the
	 * given container.
	 *
	 * @param pageable the paging metadata information
	 * @param containerId the containerId of the container
	 * @return the pageable {@link ModuleMetadata} of the modules.
	 */
	@Override
	public Page<ModuleMetadata> findAllByContainerId(Pageable pageable, String containerId) {
		Assert.hasLength(containerId, "containerId is required");
		List<ModuleMetadata> results = new ArrayList<ModuleMetadata>();
		try {
			List<String> deployedModules = getDeployedModules(containerId);
			for (String moduleId : deployedModules) {
				results.add(findOne(containerId, moduleId));
			}
			return pagingUtility.getPagedData(pageable, results);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * Find all module metadata by the given module id.
	 *
	 * @param pageable the paging metadata information
	 * @param moduleId the module id to use
	 * @return the pageable {@link ModuleMetadata}
	 */
	@Override
	public Page<ModuleMetadata> findAllByModuleId(Pageable pageable, String moduleId) {
		Assert.hasLength(moduleId, "moduleId is required");
		try {
			List<ModuleMetadata> results = new ArrayList<ModuleMetadata>();
			for (String containerId : getAvailableContainerIds()) {
				ModuleMetadata metadata = findOne(containerId, moduleId);
				if (metadata != null) {
					results.add(metadata);
				}
			}
			return pagingUtility.getPagedData(pageable, results);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * Get all the deployed modules by the given containerId.
	 *
	 * @param containerId the containerId to filter
	 * @return the list of moduleIds of the deployed modules.
	 */
	private List<String> getDeployedModules(String containerId) {
		try {
			CuratorFramework client = zkConnection.getClient();
			if (null != client.checkExists().forPath(Paths.build(Paths.CONTAINERS, containerId))) {
				return client.getChildren().forPath(path(containerId));
			}
		}
		catch (Exception e) {
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return Collections.emptyList();
	}

	@Override
	public Iterable<ModuleMetadata> findAll(Iterable<String> ids) {
		List<ModuleMetadata> results = new ArrayList<ModuleMetadata>();
		for (String id : ids) {
			ModuleMetadata entity = findOne(id);
			if (entity != null) {
				results.add(entity);
			}
		}
		return results;
	}

	@Override
	public long count() {
		long count = 0;
		try {
			List<String> containerIds = zkConnection.getClient().getChildren().forPath(Paths.MODULE_DEPLOYMENTS);
			for (String containerId : containerIds) {
				List<String> modules = zkConnection.getClient().getChildren().forPath(path(containerId));
				count = count + modules.size();
			}
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return count;
	}

	@Override
	public Iterable<ModuleMetadata> findAllInRange(String from,
			boolean fromInclusive, String to, boolean toInclusive) {
		throw new UnsupportedOperationException("Not supported.");
	}

	private String path(String id) {
		return Paths.build(Paths.MODULE_DEPLOYMENTS, id);
	}

	private String metadataPath(String containerId, String moduleId) {
		return Paths.build(Paths.MODULE_DEPLOYMENTS, containerId, moduleId, "metadata");
	}

	@Override
	public <S extends ModuleMetadata> S save(S entity) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void delete(String id) {
	}

	@Override
	public void delete(ModuleMetadata entity) {
	}

	@Override
	public void delete(Iterable<? extends ModuleMetadata> entities) {
	}

	@Override
	public void deleteAll() {
	}
}
