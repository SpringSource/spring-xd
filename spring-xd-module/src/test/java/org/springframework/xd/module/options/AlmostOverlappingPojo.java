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

package org.springframework.xd.module.options;

import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * A Pojo that has an option that is identical to
 * with {@link org.springframework.xd.module.options.BackingPojo#setFoo(String)} but has a
 * different description.
 *
 * @author Mark Pollack
 */
public class AlmostOverlappingPojo {

	private String foo = "somedefault";

	public String getFoo() {
		return foo;
	}

	@ModuleOption("Another foo option")
	public void setFoo(String foo) {
		this.foo = foo;
	}

}
