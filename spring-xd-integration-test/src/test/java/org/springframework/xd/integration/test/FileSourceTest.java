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

package org.springframework.xd.integration.test;

import java.util.UUID;

import org.junit.Test;

import org.springframework.xd.integration.fixtures.FileSink;


/**
 * 
 * @author renfrg
 */
public class FileSourceTest extends AbstractIntegrationTest {

	/**
	 * @throws Exception
	 */
	public FileSourceTest() throws Exception {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void testFileSource() throws Exception {
		String data = UUID.randomUUID().toString();
		String sourceDir = UUID.randomUUID().toString();
		String fileName = UUID.randomUUID().toString();

		stream(sources.file(sourceDir, fileName + ".out") + XD_DELIMETER
				+ sinks.getSink(FileSink.class));
		stream("dataSender",
				"trigger  --payload='" + data + "'" + XD_DELIMETER
						+ sinks.file(sourceDir, fileName).toDSL("REPLACE", "true"));

		assertValid(data, sinks.getSink(FileSink.class));
	}

	@Test
	public void testTailSource() throws Exception {
		String data = UUID.randomUUID().toString();
		String sourceDir = UUID.randomUUID().toString();
		String fileName = UUID.randomUUID().toString();

		stream(sources.tail(1000, sourceDir + "/" + fileName + ".out") + XD_DELIMETER
				+ sinks.getSink(FileSink.class));
		stream("dataSender", sources.http() + XD_DELIMETER + sinks.file(sourceDir, fileName).toDSL("REPLACE", "false"));
		sources.http().postData(data);
		assertValid(data, sinks.getSink(FileSink.class));
	}

}
