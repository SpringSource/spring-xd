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
package org.springframework.xd.analytics.model.jpmml;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.xd.tuple.Tuple;

/**
 * Author: Thomas Darimont
 */
public class AssociationJpmmlAnalyticalModelEvaluatorTests extends AbstractJpmmlAnalyticalModelEvaluatorTests {

	@Before
	public void setup() throws Exception {

		analyticalModelEvaluator = new JpmmlAnalyticalModelEvaluator();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEvaluateAssociationRules1shopping() throws Exception {

		useModel("association-rules-1-shopping.pmml.xml", null, Arrays.asList("Predicted_item"));

		Tuple output = analyticalModelEvaluator.evaluate(objectToTuple(new Object() {
			Collection<String> item= Arrays.asList("Choclates");
		}));

		Collection<String> predicted = (Collection<String>)output.getValue("Predicted_item");
		assertThat(predicted, hasItems("Pencil"));
	}
}
