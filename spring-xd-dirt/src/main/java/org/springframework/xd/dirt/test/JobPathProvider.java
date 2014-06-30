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

package org.springframework.xd.dirt.test;


import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.stream.JobRepository;
import org.springframework.xd.dirt.zookeeper.Paths;

/**
 * Provides path information for job definitions, deployments.
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 */
public class JobPathProvider implements DeploymentPathProvider {

	/**
	 * Job instance repository
	 */
	private final JobRepository jobRepository;

	/**
	 * Construct a JobPathProvider.
	 *
	 * @param jobRepository the job instance repository
	 */
	public JobPathProvider(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefinitionPath(String jobName) {
		return Paths.build(Paths.JOBS, jobName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DeploymentUnitStatus getDeploymentStatus(String jobName) {
		return this.jobRepository.getDeploymentStatus(jobName);
	}
}
