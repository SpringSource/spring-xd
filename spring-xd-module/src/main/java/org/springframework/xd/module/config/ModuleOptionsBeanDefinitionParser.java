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

package org.springframework.xd.module.config;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleOptions.ProfileActivationRule;


/**
 * Parses the top-level {@code <xd:options ...>} XML element, emitting a {@link ModuleOptions}.
 * 
 * @author Eric Bottard
 */
public class ModuleOptionsBeanDefinitionParser implements BeanDefinitionParser {

	private static final Map<String, String> SHORT_TYPE_NAMES = new HashMap<String, String>();
	static {
		SHORT_TYPE_NAMES.put("string", String.class.getName());
		SHORT_TYPE_NAMES.put("boolean", Boolean.class.getName());
		SHORT_TYPE_NAMES.put("byte", Byte.class.getName());
		SHORT_TYPE_NAMES.put("char", Character.class.getName());
		SHORT_TYPE_NAMES.put("short", Short.class.getName());
		SHORT_TYPE_NAMES.put("int", Integer.class.getName());
		SHORT_TYPE_NAMES.put("long", Long.class.getName());
		SHORT_TYPE_NAMES.put("float", Float.class.getName());
		SHORT_TYPE_NAMES.put("double", Double.class.getName());
		SHORT_TYPE_NAMES.put("date", Date.class.getName());
	}

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ModuleOptions.class);

		registerModuleOptions(element, parserContext, builder);
		registerProfileActivations(element, parserContext, builder);

		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());

		return builder.getBeanDefinition();
	}

	private void registerProfileActivations(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		List<Element> profiles = DomUtils.getChildElementsByTagName(element, "profile");

		ManagedList<BeanMetadataElement> profilesList = new ManagedList<BeanMetadataElement>();
		for (Element profileElement : profiles) {
			BeanDefinitionBuilder ruleBuilder = BeanDefinitionBuilder.genericBeanDefinition(ProfileActivationRule.class);
			ruleBuilder.addPropertyValue("profile", profileElement.getAttribute("name"));
			ruleBuilder.addPropertyValue("rule", profileElement.getAttribute("rule"));
			profilesList.add(ruleBuilder.getBeanDefinition());
		}
		builder.addPropertyValue("profileRules", profilesList);
	}

	private void registerModuleOptions(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		List<Element> options = DomUtils.getChildElementsByTagName(element, "option");

		ManagedList<BeanMetadataElement> optionsList = new ManagedList<BeanMetadataElement>();
		for (Element optionElement : options) {
			BeanDefinitionBuilder optionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ModuleOption.class);
			optionBuilder.addConstructorArgValue(optionElement.getAttribute("name"));
			optionBuilder.addPropertyValue("description", optionElement.getAttribute("description"));
			if (optionElement.hasAttribute("type")) {
				optionBuilder.addPropertyValue("type", shortToFQDN(optionElement.getAttribute("type")));
			}

			boolean hasDefaultValue = optionElement.hasAttribute("default-value");
			boolean hasDefaultExpression = optionElement.hasAttribute("default-expression");
			if (hasDefaultValue && hasDefaultExpression) {
				parserContext.getReaderContext().error(
						"Only one of 'default-value' or 'default-expression' is allowed",
						optionElement.getAttributeNode("default-value"));
			}
			if (hasDefaultValue) {
				optionBuilder.addPropertyValue("defaultValue", optionElement.getAttribute("default-value"));
			}
			else if (hasDefaultExpression) {
				optionBuilder.addPropertyValue("defaultExpression", optionElement.getAttribute("default-expression"));
			}

			optionsList.add(optionBuilder.getBeanDefinition());
		}

		builder.addPropertyValue("options", optionsList);

	}

	private String shortToFQDN(String type) {
		String longer = SHORT_TYPE_NAMES.get(type);
		return longer == null ? type : longer;
	}

}
