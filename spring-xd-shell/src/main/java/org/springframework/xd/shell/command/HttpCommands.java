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

package org.springframework.xd.shell.command;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.shell.XDShell;

/**
 * Http commands.
 * 
 * @author Jon Brisbin
 * @author Ilayaperumal Gopinathan
 */

@Component
public class HttpCommands implements CommandMarker {

	private static final String POST_HTTPSOURCE = "post httpsource";
	
	@Autowired
	private XDShell xdShell;

	@CliAvailabilityIndicator({ POST_HTTPSOURCE })
	public boolean available() {
		return xdShell.getSpringXDOperations() != null;
	}

	@CliCommand(value = { POST_HTTPSOURCE }, help = "POST data to http endpoint")
	public String postHttp(
			@CliOption(mandatory = true, key = "target") String target,
			@CliOption(mandatory = true, key = "data") String data) {
		final StringBuilder buffer = new StringBuilder();
		URI requestURI = URI.create(target);
		RestTemplate restTemplate = new RestTemplate();
		try {
			restTemplate.setErrorHandler(new ResponseErrorHandler() {

				@Override
				public boolean hasError(ClientHttpResponse response)
						throws IOException {
					HttpStatus status = response.getStatusCode();
					return (status == HttpStatus.BAD_GATEWAY
							|| status == HttpStatus.GATEWAY_TIMEOUT || status == HttpStatus.INTERNAL_SERVER_ERROR);
				}

				@Override
				public void handleError(ClientHttpResponse response)
						throws IOException {
					outputError(response.getStatusCode(), buffer);
				}
			});
			outputRequest("POST", requestURI, data, buffer);
			ResponseEntity<String> response = restTemplate.postForEntity(
					requestURI, data, String.class);
			outputResponse(response, buffer);
			String status = (response.getStatusCode().equals(HttpStatus.OK) ? "Success"
					: "Error");
			return String.format(buffer.toString() + status
					+ " sending data '%s' to target '%s'", data, target);
		} 
		catch (ResourceAccessException e) {
			return String.format(buffer.toString()
					+ "Failed to access http endpoint %s", target);
		} 
		catch (Exception e) {
			return String.format(buffer.toString()
					+ "Failed to send data to http endpoint %s", target);
		}
	}
	
	private void outputRequest(String method, URI requestUri,
			String requestData, StringBuilder buffer) {
		buffer.append("> ")
			  .append(method)
			  .append(" ")
			  .append(requestUri.toString())
			  .append(" ")
			  .append(requestData)
			  .append(OsUtils.LINE_SEPARATOR);
	}

	private void outputResponse(ResponseEntity<String> response,
			StringBuilder buffer) {
		buffer.append("> ")
		      .append(response.getStatusCode().value())
		      .append(" ")
		      .append(response.getStatusCode().name())
			  .append(OsUtils.LINE_SEPARATOR);
		for (Map.Entry<String, List<String>> entry : response.getHeaders()
				.entrySet()) {
			buffer.append("> ").append(entry.getKey()).append(": ");
			boolean first = true;
			for (String s : entry.getValue()) {
				if (!first) {
					buffer.append(",");
				} else {
					first = false;
				}
				buffer.append(s);
			}
			buffer.append(OsUtils.LINE_SEPARATOR);
		}
		buffer.append("> ").append(OsUtils.LINE_SEPARATOR);
		if (null != response.getBody()) {
			buffer.append(response.getBody());
		}
	}

	private void outputError(HttpStatus status, StringBuilder buffer) {
		buffer.append("> ")
		      .append(status.value())
		      .append(" ")
			  .append(status.name())
			  .append(OsUtils.LINE_SEPARATOR);
	}

}
