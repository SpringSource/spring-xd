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

package org.springframework.xd.analytics.metrics;

import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class AbstractCounterRepositoryTests {

	public void simpleTest(CounterRepository repo) {
		Counter counter = new Counter("simpleCounter");

		String counterName = counter.getName();
		assertThat(counterName, equalTo("simpleCounter"));

		repo.increment(counterName);
		Counter c = repo.findOne(counterName);
		assertThat(c.getValue(), equalTo(1L));

		repo.increment(counterName);
		assertThat(repo.findOne(counterName).getValue(), equalTo(2L));

		repo.decrement(counterName);
		assertThat(repo.findOne(counterName).getValue(), equalTo(1L));

		repo.reset(counterName);
		assertThat(repo.findOne(counterName).getValue(), equalTo(0L));

		Counter counter2 = repo.findOne("simpleCounter");
		assertThat(counter, equalTo(counter2));

	}

}
