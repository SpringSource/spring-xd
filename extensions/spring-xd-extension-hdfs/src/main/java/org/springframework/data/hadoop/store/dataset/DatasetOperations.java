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
package org.springframework.data.hadoop.store.dataset;

import com.cloudera.cdk.data.PartitionStrategy;

import java.util.Collection;

/**
 * Interface specifying a basic set of {@link com.cloudera.cdk.data.Dataset} operations against a specific
 * {@link com.cloudera.cdk.data.DatasetRepository}. Implemented by DatasetTemplate.
 *
 * @author Thomas Risberg
 * @since 1.0
 */
public interface DatasetOperations {

	/**
	 * Read all records in the dataset and call the provided callback for each record.
	 *
	 * @param targetClass the class that is stored in the dataset
	 * @param callback the callback to be called for each record
	 */
	<T> void read(Class<T> targetClass, RecordCallback<T> callback);

	/**
	 * Read all records in the dataset and call the provided callback for each record.
	 *
	 * @param targetClass the class that is stored in the dataset
	 * @return cloection containg the records as the specified target class
	 */
	<T> Collection<T> read(Class<T> targetClass);

	/**
	 * Write all records provided in the record collection using the provided
	 * partition strategy
	 *
	 * @param records the records to write
	 * @param partitionStrategy the partition strategy
	 */
	void write(Collection<?> records, PartitionStrategy partitionStrategy);

	/**
	 * Write all records provided in the record collection without partitioning
	 *
	 * @param records the records to write
	 */
	void write(Collection<?> records);

	/**
	 * Execute a callback for the {@link com.cloudera.cdk.data.DatasetRepository}
 	 * @param callback
	 */
	void execute(DatasetRepositoryCallback callback);

	/**
	 * Get the dataset name to be used for the given class
	 * @param clazz the class stored in the dataset
	 * @return the dataset name
	 */
	<T> String getDatasetName(Class<T> clazz);
}
