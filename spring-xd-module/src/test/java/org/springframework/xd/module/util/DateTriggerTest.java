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

package org.springframework.xd.module.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;


/**
 * 
 * @author Glenn Renfro
 */
public class DateTriggerTest {

	@Test
	public void testEmptyList() {
		boolean exceptionFired = false;
		try {
			new DateTrigger((Date) null);
		}
		catch (IllegalArgumentException ipe) {
			exceptionFired = true;
		}
		assertTrue("A null constructor entry should cause DateTrigger to fire a IllegalArgumentException",
				exceptionFired);
	}

	@Test
	public void testDefaultConstructor() {
		DateTrigger dateTrigger = new DateTrigger();
		Date nextExecutionTime = dateTrigger.nextExecutionTime(null);
		assertNotNull("The next Execution Time must have a value from a default constructor.", nextExecutionTime);
		nextExecutionTime = dateTrigger.nextExecutionTime(null);
		assertNull("The default constructor has only one entry and thus, this value should have already been pulled.",
				nextExecutionTime);

	}

	@Test
	public void testConstructor() {
		Date epoch = new Date(0);
		Calendar currentCalendar = Calendar.getInstance();
		Date current = currentCalendar.getTime();
		currentCalendar.add(Calendar.HOUR, -1);
		Date past = currentCalendar.getTime();
		currentCalendar.add(Calendar.HOUR, 2);
		Date future = currentCalendar.getTime();
		DateTrigger dateTrigger = new DateTrigger(current, epoch, future, past);

		Date nextExecutionTime = dateTrigger.nextExecutionTime(null);
		assertNotNull("Should return epoch", nextExecutionTime);
		assertTrue("Should be epoch", epoch.compareTo(nextExecutionTime) == 0);

		nextExecutionTime = dateTrigger.nextExecutionTime(null);
		assertNotNull("Should return past", nextExecutionTime);
		assertTrue("Should be past", past.compareTo(nextExecutionTime) == 0);

		nextExecutionTime = dateTrigger.nextExecutionTime(null);
		assertNotNull("Should return current", nextExecutionTime);
		assertTrue("Should be current", current.compareTo(nextExecutionTime) == 0);

		nextExecutionTime = dateTrigger.nextExecutionTime(null);
		assertNotNull("Should return future", nextExecutionTime);
		assertTrue("Should be future", future.compareTo(nextExecutionTime) == 0);

		nextExecutionTime = dateTrigger.nextExecutionTime(null);
		assertNull("All entries should have been pulled, the nextExecutionTime should have been null.",
				nextExecutionTime);

	}

}
