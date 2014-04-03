/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.mixins.BatchJobFieldNamesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobResourcesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Options for Hdfs to Mongodb export job module.
 * 
 * @author Ilayaperumal Gopinathan
 */
@Mixin({ BatchJobRestartableOptionMixin.class, BatchJobResourcesOptionMixin.class, BatchJobFieldNamesOptionMixin.class })
public class HdfsMongodbJobOptionsMetadata {

	private String databaseName = "xd";

	private String host;

	private String port;

	private String collectionName;

	private String idField = "null";

	@ModuleOption("the Mongodb collection to store")
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	@ModuleOption("the Mongodb database name")
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	@ModuleOption("the Mongodb host")
	public void setHost(String host) {
		this.host = host;
	}

	@ModuleOption("the Mongodb port")
	public void setPort(String port) {
		this.port = port;
	}

	@ModuleOption("the name of the field to use as the identity in Mongodb")
	public void setIdField(String idField) {
		this.idField = idField;
	}

	public String getCollectionName() {
		return this.collectionName;
	}

	public String getDatabaseName() {
		return this.databaseName;
	}

	public String getHost() {
		return this.host;
	}

	public String getPort() {
		return this.port;
	}

	public String getIdField() {
		return this.idField;
	}

}
