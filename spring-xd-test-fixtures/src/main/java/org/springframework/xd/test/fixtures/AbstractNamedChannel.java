/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.test.fixtures;


/**
 * Base class for named channel fixtures that can be incorporated in a stream definition by calling their {@link #toString()} method.
 * 
 * @author Glenn Renfro
 */
public abstract class AbstractNamedChannel<F extends AbstractNamedChannel<F>> {

	@Override
	public final String toString() {
		return toDSL();
	}

	/**
	 * Returns a representation of the named channel suitable for inclusion in a stream definition, <i>e.g.</i>
	 * {@code tap:stream:streamname.label}
	 */
	protected abstract String toDSL();
}
