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

import static org.junit.Assert.*;
import static org.springframework.xd.tuple.TupleBuilder.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.xd.tuple.Tuple;

/**
 * Author: Thomas Darimont
 */
public class RegressionJpmmlAnalyticalModelEvaluatorTests extends AbstractJpmmlAnalyticalModelEvaluatorTests {

	@Before
	public void setup() throws Exception {

		analyticalModelEvaluator = new JpmmlAnalyticalModelEvaluator();
	}

	@Test
	public void testEvaluateSimpleLinearRegression1_should_add_rate_field_in_output() throws Exception {

		useModel("simple-linear-regression-1.pmml.xml", null, Arrays.asList("rate"));

		Tuple input = tuple().of("year", 2015);

		Tuple output = analyticalModelEvaluator.evaluate(input);

		assertEquals(-1.367, output.getDouble("rate"), 0.0001);
	}

	@Test
	public void testEvaluateSimpleLinearRegression1_should_replace_rate_field_in_output() throws Exception {

		useModel("simple-linear-regression-1.pmml.xml", null, Arrays.asList("rate"));

		Tuple input = tuple().of("year", 2015, "rate", -1);

		Tuple output = analyticalModelEvaluator.evaluate(input);

		assertEquals(-1.367, output.getDouble("rate"), 0.0001);
	}

	@Test
	public void testEvaluateAdvancedLinearRegression1Iris() throws Exception{

		useModel("simple-linear-regression-2-iris.pmml.xml", null, Arrays.asList("Petal.Width"));

		Tuple input = tuple().of("Petal.Length", 4.5);

		Tuple output = analyticalModelEvaluator.evaluate(input);

		assertEquals(1.5104, output.getDouble("Petal.Width"), 0.0001);
	}
}
