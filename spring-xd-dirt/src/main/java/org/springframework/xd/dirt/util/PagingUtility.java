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

package org.springframework.xd.dirt.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;


/**
 * Utility class for paging support in repository accessor methods.
 *
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class PagingUtility<T extends Comparable<? super T>> {

	/**
	 * Get the paged data of the given list.
	 *
	 * @param pageable the paging metadata
	 * @param list the list of content to paginate
	 * @return the paginated implementation of the given list of <T>
	 */
	public Page<T> getPagedData(Pageable pageable, List<T> list) {

		if (CollectionUtils.isEmpty(list)) {
			return new PageImpl<T>(list);
		}

		Collections.sort(list);

		int offSet = pageable.getOffset();
		int size = pageable.getPageSize();

		List<T> page = new ArrayList<T>();
		for (int i = offSet; i < Math.min(list.size(), offSet + size); i++) {
			page.add(list.get(i));
		}
		return new PageImpl<T>(page, pageable, list.size());
	}

}
