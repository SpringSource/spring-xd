
package org.springframework.xd.dirt.server;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.XDContainer;
import org.springframework.xd.dirt.server.options.ContainerOptions;
import org.springframework.xd.dirt.server.options.XDPropertyKeys;
import org.springframework.xd.dirt.util.BannerUtils;
import org.springframework.xd.dirt.util.XdConfigLoggingInitializer;

@Configuration
@EnableAutoConfiguration
@ImportResource({
	"classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "launcher.xml",
	"classpath:" + XDContainer.XD_INTERNAL_CONFIG_ROOT + "container.xml",
	"classpath*:" + XDContainer.XD_CONFIG_ROOT + "plugins/*.xml" })
public class LauncherApplication {

	public static final String NODE_PROFILE = "node";

	private ConfigurableApplicationContext context;

	public static void main(String[] args) {
		new LauncherApplication().run(args);
	}

	public ConfigurableApplicationContext getContext() {
		return this.context;
	}

	public LauncherApplication run(String... args) {
		System.out.println(BannerUtils.displayBanner(getClass().getSimpleName(), null));
		try {
			this.context = new SpringApplicationBuilder(ContainerOptions.class, ParentConfiguration.class)
					.profiles(NODE_PROFILE)
					.child(LauncherApplication.class).run(args);
		}
		catch (Exception e) {
			handleErrors(e);

		}
		publishContainerStarted(context);
		return this;
	}

	private void handleErrors(Exception e) {
		if (e.getCause() instanceof BindException) {
			BindException be = (BindException) e.getCause();
			for (FieldError error : be.getFieldErrors()) {
				System.err.println(String.format("the value '%s' is not allowed for property '%s'",
						error.getRejectedValue(),
						error.getField()));
				if (XDPropertyKeys.XD_CONTROL_TRANSPORT.equals(error.getField())) {
					System.err.println(
							String.format(
									"If not explicitly provided, the default value of '%s' assumes the value provided for '%s'",
									XDPropertyKeys.XD_CONTROL_TRANSPORT, XDPropertyKeys.XD_TRANSPORT,
									error.getRejectedValue()));
				}
			}
		}
		System.exit(1);
	}

	public static void publishContainerStarted(ConfigurableApplicationContext context) {
		XDContainer container = new XDContainer();
		context.setId(container.getId());
		container.setContext(context);
		context.publishEvent(new ContainerStartedEvent(container));
	}

	@Bean
	public ApplicationListener<?> xdInitializer(ApplicationContext context) {
		XdConfigLoggingInitializer delegate = new XdConfigLoggingInitializer(true);
		delegate.setEnvironment(context.getEnvironment());
		return new SourceFilteringListener(context, delegate);
	}

	@ConditionalOnExpression("${XD_JMX_ENABLED:false}")
	@EnableMBeanExport(defaultDomain = "xd.container")
	protected static class JmxConfiguration {

		@Bean
		public IntegrationMBeanExporter integrationMBeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			exporter.setDefaultDomain("xd.container");
			return exporter;
		}
	}

}
