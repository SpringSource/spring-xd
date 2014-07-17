/*
 * Copyright 2011-2014 the original author or authors.
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

package org.springframework.xd.integration.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.AWSEC2Api;
import org.jclouds.aws.ec2.domain.AWSRunningInstance;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.domain.Reservation;
import org.jclouds.ec2.domain.RunningInstance;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.sshj.SshjSshClient;

import org.springframework.hateoas.PagedResources;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.ContainerResource;
import org.springframework.xd.rest.domain.ModuleMetadataResource;
import org.springframework.xd.rest.domain.StreamDefinitionResource;

import com.google.common.collect.Iterables;
import com.google.common.net.HostAndPort;

/**
 * Utilities for creating and monitoring streams and the JMX hooks for those strings.
 *
 * @author Glenn Renfro
 */
public class StreamUtils {

	public final static String TMP_DIR = "result/";

	/**
	 * Creates the stream definition and deploys it to the cluster being tested.
	 *
	 * @param streamName The name of the stream
	 * @param streamDefinition The definition that needs to be deployed for this stream.
	 * @param adminServer The admin server that this stream will be deployed against.
	 */
	public static void stream(final String streamName, final String streamDefinition,
			final URL adminServer) {
		Assert.hasText(streamName, "The stream name must be specified.");
		Assert.hasText(streamDefinition, "a stream definition must be supplied.");
		Assert.notNull(adminServer, "The admin server must be specified.");
		createSpringXDTemplate(adminServer).streamOperations().createStream(streamName, streamDefinition, true);
	}

	/**
	 * Executes a http get for the client and returns the results as a string
	 *
	 * @param url The location to execute the get against.
	 * @return the string result of the get
	 */
	public static String httpGet(final URL url) {
		Assert.notNull(url, "The URL must be specified");
		RestTemplate template = new RestTemplate();

		try {
			String result = template.getForObject(url.toURI(), String.class);
			return result;
		}
		catch (URISyntaxException uriException) {
			throw new IllegalStateException(uriException.getMessage(), uriException);
		}
	}

	/**
	 * Removes all the streams from the cluster. Used to guarantee a clean acceptance test.
	 *
	 * @param adminServer The admin server that the command will be executed against.
	 */
	public static void destroyAllStreams(final URL adminServer) {
		Assert.notNull(adminServer, "The admin server must be specified.");
		createSpringXDTemplate(adminServer).streamOperations().destroyAll();
	}

	/**
	 * Undeploys the specified stream name
	 *
	 * @param adminServer The admin server that the command will be executed against.
	 * @param streamName The name of the stream to undeploy
	 */
	public static void undeployStream(final URL adminServer, final String streamName)
	{
		Assert.notNull(adminServer, "The admin server must be specified.");
		Assert.hasText(streamName, "The streamName must not be empty nor null");
		createSpringXDTemplate(adminServer).streamOperations().undeploy(streamName);
	}

	/**
	 * Copies the specified file from a remote machine to local machine.
	 *
	 * @param xdEnvironment The environment configuration for this test
	 * @param url The remote machine's url.
	 * @param fileName The fully qualified file name of the file to be transferred.
	 * @return The location to the fully qualified file name where the remote file was copied.
	 */
	public static String transferResultsToLocal(final XdEnvironment xdEnvironment, final URL url, final String fileName)
	{
		Assert.notNull(xdEnvironment, "The Acceptance Test, require a valid xdEnvironment.");
		Assert.notNull(url, "The remote machine's URL must be specified.");
		Assert.hasText(fileName, "The remote file name must be specified.");

		File file = new File(fileName);
		FileOutputStream fileOutputStream = null;
		InputStream inputStream = null;

		try {
			File tmpFile = createTmpDir();
			String fileLocation = tmpFile.getAbsolutePath() + file.getName();
			fileOutputStream = new FileOutputStream(fileLocation);

			final LoginCredentials credential = LoginCredentials
					.fromCredentials(new Credentials("ubuntu", xdEnvironment.getPrivateKey()));
			final HostAndPort socket = HostAndPort.fromParts(url.getHost(), 22);
			final SshjSshClient client = new SshjSshClient(
					new BackoffLimitedRetryHandler(), socket, credential, 5000);

			inputStream = client.get(fileName).openStream();

			FileCopyUtils.copy(inputStream, fileOutputStream);
			return fileLocation;
		}
		catch (IOException ioException) {
			throw new IllegalStateException(ioException.getMessage(), ioException);
		}
	}

	/**
	 * Creates a file on a remote EC2 machine with the payload as its contents.
	 *
	 * @param xdEnvironment The environment configuration for this test
	 * @param host The remote machine's ip.
	 * @param dir The directory to write the file
	 * @param fileName The fully qualified file name of the file to be created.
	 * @param payload the data to write to the file
	 */
	public static void createDataFileOnRemote(XdEnvironment xdEnvironment, String host, String dir, String fileName,
			String payload)
	{
		Assert.notNull(xdEnvironment, "The Acceptance Test, require a valid xdEnvironment.");
		Assert.hasText(host, "The remote machine's URL must be specified.");
		Assert.notNull(dir, "dir should not be null");
		Assert.hasText(fileName, "The remote file name must be specified.");

		final LoginCredentials credential = LoginCredentials
				.fromCredentials(new Credentials("ubuntu", xdEnvironment.getPrivateKey()));
		final HostAndPort socket = HostAndPort.fromParts(host, 22);
		final SshjSshClient client = new SshjSshClient(
				new BackoffLimitedRetryHandler(), socket, credential, 5000);
		client.exec("mkdir " + dir);
		client.put(dir + "/" + fileName, payload);
	}

	/**
	 * Appends the payload to an existing file on a remote EC2 Instance.
	 *
	 * @param xdEnvironment The environment configuration for this test
	 * @param host The remote machine's ip.
	 * @param dir The directory to write the file
	 * @param fileName The fully qualified file name of the file to be created.
	 * @param payload the data to append to the file
	 */
	public static void appendToRemoteFile(XdEnvironment xdEnvironment, String host, String dir, String fileName,
			String payload)
	{
		Assert.notNull(xdEnvironment, "The Acceptance Test, require a valid xdEnvironment.");
		Assert.hasText(host, "The remote machine's URL must be specified.");
		Assert.notNull(dir, "dir should not be null");
		Assert.hasText(fileName, "The remote file name must be specified.");

		final LoginCredentials credential = LoginCredentials
				.fromCredentials(new Credentials("ubuntu", xdEnvironment.getPrivateKey()));
		final HostAndPort socket = HostAndPort.fromParts(host, 22);
		final SshjSshClient client = new SshjSshClient(
				new BackoffLimitedRetryHandler(), socket, credential, 5000);
		client.exec("echo '" + payload + "' >> " + dir + "/" + fileName);
	}

	/**
	 * Returns a list of active instances from the specified ec2 region.
	 * @param awsAccessKey the unique id of the ec2 user.
	 * @param awsSecretKey the password of ec2 user.
	 * @param awsRegion The aws region to inspect for acceptance test instances.
	 * @return a list of active instances in the account and region specified.
	 */
	public static List<RunningInstance> getEC2RunningInstances(String awsAccessKey, String awsSecretKey,
			String awsRegion) {
		Assert.hasText(awsAccessKey, "awsAccessKey must not be empty nor null");
		Assert.hasText(awsSecretKey, "awsSecretKey must not be empty nor null");
		Assert.hasText(awsRegion, "awsRegion must not be empty nor null");

		AWSEC2Api client = ContextBuilder.newBuilder("aws-ec2")
				.credentials(awsAccessKey, awsSecretKey)
				.buildApi(AWSEC2Api.class);
		Set<? extends Reservation<? extends AWSRunningInstance>> reservations = client
				.getInstanceApi().get().describeInstancesInRegion(awsRegion);
		int instanceCount = reservations.size();
		ArrayList<RunningInstance> result = new ArrayList<RunningInstance>();
		for (int awsRunningInstanceCount = 0; awsRunningInstanceCount < instanceCount; awsRunningInstanceCount++) {
			Reservation<? extends AWSRunningInstance> instances = Iterables
					.get(reservations, awsRunningInstanceCount);
			int groupCount = instances.size();
			for (int runningInstanceCount = 0; runningInstanceCount < groupCount; runningInstanceCount++) {
				result.add(Iterables.get(instances, runningInstanceCount));
			}
		}
		return result;
	}

	/**
	 * Substitutes the port associated with the URL with another port.
	 *
	 * @param url The URL that needs a port replaced.
	 * @param port The new port number
	 * @return A new URL with the host from the URL passed in and the new port.
	 */
	public static URL replacePort(final URL url, final int port) {
		Assert.notNull(url, "the url must not be null");
		try {
			return new URL("http://" + url.getHost() + ":" + port);
		}
		catch (MalformedURLException malformedUrlException) {
			throw new IllegalStateException(malformedUrlException.getMessage(), malformedUrlException);
		}
	}

	/**
	 * Creates a map of container Id's and the associated host.
	 * @param adminServer The admin server to be queried.
	 * @return Map where the key is the container id and the value is the host ip.
	 */
	public static Map<String, String> getAvailableContainers(URL adminServer) {
		Assert.notNull(adminServer, "adminServer must not be null");
		HashMap<String, String> results = new HashMap<String, String>();
		Iterator<ContainerResource> iter = createSpringXDTemplate(adminServer).runtimeOperations().listRuntimeContainers().iterator();
		while (iter.hasNext()) {
			ContainerResource container = iter.next();
			results.put(container.getAttribute("id"), container.getAttribute("host"));
		}
		return results;
	}

	/**
	 * Return a list of container id's where the module is deployed
	 * @param adminServer The admin server that will be queried.
	 * @param moduleIdPrefix The admin server will uses this module id prefix to find the containers where the module prefix is deployed.
	 * @return A list of containers where the module is deployed.
	 */
	public static PagedResources<ModuleMetadataResource> getRuntimeModules(URL adminServer) {
		Assert.notNull(adminServer, "adminServer must not be null");
		return createSpringXDTemplate(adminServer).runtimeOperations().listRuntimeModules();
	}

	/**
	 * Waits up to the wait time for a stream to be deployed.
	 *
	 * @param streamName The name of the stream to be evaluated.
	 * @param adminServer The admin server URL that will be queried.
	 * @param waitTime the amount of time in millis to wait.
	 * @return true if the stream is deployed else false.
	 */
	public static boolean waitForStreamDeployment(String streamName, URL adminServer, int waitTime) {
		boolean result = isStreamDeployed(streamName, adminServer);
		long timeout = System.currentTimeMillis() + waitTime;
		while (!result && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}
			result = isStreamDeployed(streamName, adminServer);
		}

		return result;
	}

	/**
	 * Checks to see if the specified stream is deployed on the XD cluster.
	 *
	 * @param streamName The name of the stream to be evaluated.
	 * @param adminServer The admin server URL that will be queried.
	 * @return true if the stream is deployed else false
	 */
	public static boolean isStreamDeployed(String streamName, URL adminServer) {
		Assert.hasText(streamName, "The stream name must be specified.");
		Assert.notNull(adminServer, "The admin server must be specified.");
		boolean result = false;
		SpringXDTemplate xdTemplate = createSpringXDTemplate(adminServer);
		PagedResources<StreamDefinitionResource> resources = xdTemplate.streamOperations().list();
		Iterator<StreamDefinitionResource> resourceIter = resources.iterator();
		while (resourceIter.hasNext()) {
			StreamDefinitionResource resource = resourceIter.next();
			if (streamName.equals(resource.getName())) {
				if ("deployed".equals(resource.getStatus())) {
					result = true;
					break;
				}
				else {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	private static File createTmpDir() throws IOException {
		File tmpFile = new File(System.getProperty("user.dir") + "/" + TMP_DIR);
		if (!tmpFile.exists()) {
			tmpFile.createNewFile();
		}
		return tmpFile;
	}

	/**
	 * Create an new instance of the SpringXDTemplate given the Admin Server URL
	 *
	 * @param adminServer URL of the Admin Server
	 * @return A new instance of SpringXDTemplate
	 */
	private static SpringXDTemplate createSpringXDTemplate(URL adminServer) {
		try {
			return new SpringXDTemplate(adminServer.toURI());
		}
		catch (URISyntaxException uriException) {
			throw new IllegalStateException(uriException.getMessage(), uriException);
		}
	}


}
