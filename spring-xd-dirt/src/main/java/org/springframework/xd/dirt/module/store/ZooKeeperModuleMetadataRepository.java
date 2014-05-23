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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.util.MapBytesUtility;
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
		// todo: add support for paging
		return new PageImpl<ModuleMetadata>(findAll());
	}

	@Override
	public <S extends ModuleMetadata> Iterable<S> save(Iterable<S> entities) {
		List<S> results = new ArrayList<S>();
		for (S entity : entities) {
			results.add(save(entity));
		}
		return results;
	}

	private ModuleMetadata findOne(String containerId, String moduleId) {
		ModuleMetadata md = findOne(metadataPath(containerId, moduleId));
		return md;
	}

	@Override
	public ModuleMetadata findOne(String metadataPath) {
		ModuleMetadata metadata = null;
		try {
			byte[] data = zkConnection.getClient().getData().forPath(metadataPath);
			if (data != null) {
				Map<String, String> optionsMap = new HashMap<String, String>();
				Map<String, String> metadataMap = mapBytesUtility.toMap(data);
				String moduleId = getModuleId(metadataPath);
				String containerId = getContainerId(metadataPath);
				for (String propertyKey : metadataMap.keySet()) {
					String propertyValue = metadataMap.get(propertyKey);
					if (!propertyKey.startsWith(XD_MODULE_PROPERTIES_PREFIX)
							&& !StringUtils.isEmpty(propertyValue)) {
						resolveModuleOptionValues(optionsMap, metadataMap, propertyKey, propertyValue);
					}
				}
				String moduleProperties = (!optionsMap.isEmpty()) ? MapUtils.toProperties(optionsMap).toString() : "";
				metadata = new ModuleMetadata(moduleId, containerId, moduleProperties);
			}
		}
		catch (Exception e) {
			// NoNodeException - this node does not exist, will return null
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return metadata;
	}

	/**
	 * Resolve the module option value using the module metadata.
	 *
	 * @param optionsMap the options map that has resolved properties
	 * @param metadataMap the metadata map from ZK module metadata
	 * @param propertyKey the module option proeperty key
	 * @param propertyValue the module option property value to resolve
	 */
	private void resolveModuleOptionValues(Map<String, String> optionsMap,
			Map<String, String> metadataMap, String propertyKey,
			String propertyValue) {
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
			CuratorFramework client = zkConnection.getClient();
			List<String> containerIds = client.getChildren().forPath(Paths.CONTAINERS);
			for (String containerId : containerIds) {
				List<String> modules = client.getChildren().forPath(path(containerId));
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

	@Override
	public Page<ModuleMetadata> findAllByContainerId(Pageable pageable, String containerId) {
		List<ModuleMetadata> results = new ArrayList<ModuleMetadata>();
		try {
			CuratorFramework client = zkConnection.getClient();
			if (null != client.checkExists().forPath(Paths.build(Paths.CONTAINERS, containerId))) {
				List<String> modules = client.getChildren().forPath(path(containerId));
				for (String moduleId : modules) {
					results.add(findOne(containerId, moduleId));
				}
			}
			return new PageImpl<ModuleMetadata>(results);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
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
