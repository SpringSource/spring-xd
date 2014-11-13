/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import java.util.Date;
import java.util.TimeZone;

import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Gunnar Hillert
 */
@Controller
@RequestMapping("/validation")
public class ValidationController {

	@RequestMapping(value = "/cron", method = RequestMethod.POST)
	@ResponseBody
	public CronValidationDto validateCronExpression(@RequestBody CronValidationDto cronValidationDto) {

		final CronValidationDto cronValidationDtoToReturn = new CronValidationDto();
		cronValidationDtoToReturn.setCronExpression(cronValidationDto.getCronExpression());

		if (!StringUtils.hasText(cronValidationDto.getCronExpression())) {
			cronValidationDtoToReturn.setErrorMessage("The cron expression must not be empty.");
		}

		final CronTrigger cronTrigger;

		try {
			cronTrigger = new CronTrigger(cronValidationDto.getCronExpression(), TimeZone.getDefault());
			final Date nextExecutionTime = cronTrigger.nextExecutionTime(new SimpleTriggerContext());
			cronValidationDtoToReturn.setNextExecutionTime(nextExecutionTime);
		}
		catch (IllegalArgumentException e) {
			cronValidationDtoToReturn.setErrorMessage(e.getMessage());
			return cronValidationDtoToReturn;
		}
		cronValidationDtoToReturn.setValid(true);
		return cronValidationDtoToReturn;
	}

}
