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

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.core.BaseDefinition;
import org.springframework.xd.dirt.stream.AbstractDeployer;
import org.springframework.xd.dirt.stream.NoSuchDefinitionException;

/**
 * Base Class for XD Controllers.
 * 
 * @author Glenn Renfro
 * @since 1.0
 */

@SuppressWarnings("rawtypes")
public abstract class XDController<D extends BaseDefinition, V extends ResourceAssemblerSupport, T extends ResourceSupport> {

	private AbstractDeployer<D> deployer;
	private V resourceAssemblerSupport;


	protected XDController(AbstractDeployer<D> deployer,
			V resourceAssemblerSupport) {
		this.deployer = deployer;
		this.resourceAssemblerSupport = resourceAssemblerSupport;
	}

	/*
	 * Request removal of an existing module.
	 *
	 * @param name the name of an existing module (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		deployer.delete(name);
	}

	/**
	 * Request un-deployment of an existing named module.
	 *
	 * @param name
	 *            the name of an existing module (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=false")
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		getDeployer().undeploy(name);
	}

	/**
	 * Request deployment of an existing named module.
	 * 
	 * @param name
	 *            the name of an existing module (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.PUT, params = "deploy=true")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void deploy(@PathVariable("name") String name) {
		getDeployer().deploy(name);
	}

	/**
	 * Retrieve information about a single {@link ResourceSupport}.
	 * 
	 * @param name
	 *            the name of an existing tap (required)
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ResourceSupport display(@PathVariable("name") String name) {
		final D definition = getDeployer().findOne(name);
		if (definition == null) {
			throw new NoSuchDefinitionException(name,
					"There is no definition named '%s'");
		}
		return getResourceAssemblerSupport().toResource(definition);
	}

	/**
	 * List module definitions.
	 */
	@SuppressWarnings("unchecked")
	public PagedResources<T> listValues(Pageable pageable,
			PagedResourcesAssembler<D> assembler) {
		Page<D> page = getDeployer().findAll(pageable);
		if (page.hasContent()) {
			return assembler.toResource(page, getResourceAssemblerSupport());
		} else {
			return new PagedResources<T>(new ArrayList<T>(), null);
		}
	}

	/**
	 * Create a new Module.
	 * 
	 * @param name
	 *            The name of the module to create (required)
	 * @param definition
	 *            The module definition, expressed in the XD DSL (required)
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public T save(
			@RequestParam("name") String name,
			@RequestParam("definition") String definition,
			@RequestParam(value = "deploy", defaultValue = "true") boolean deploy) {
		final D moduleDefinition = definitionFactory(
				name, definition);
		final D savedModuleDefinition = getDeployer().save(moduleDefinition);
		if (deploy) {
				getDeployer().deploy(name);
		}
		@SuppressWarnings("unchecked")
		final T result = (T) getResourceAssemblerSupport().toResource(
				savedModuleDefinition);
		return result;
	}

	public AbstractDeployer<D> getDeployer() {
		return deployer;
	}

	public V getResourceAssemblerSupport() {
		return resourceAssemblerSupport;
	}

	protected abstract D definitionFactory(String name,
			String Definition);

}
