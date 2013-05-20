Spring Integration Flow
========================

*NOTE: If you want to get right to the code, see the unit tests and check out (literally :) the [spring-integration-flow-samples](http:github.com/dturanski/spring-integration-flow-samples) project*

#Goals
Spring Integration components already support common enterprise integration patterns. Sometimes it is desirable to 
create common message flows which implement higher level messaging patterns or "cross cutting concerns" for your 
application environment. This is especially true in more complex applications.

Out of the box, a best practice is to define a common message flow in its own bean definition
file which may be imported into other message flows. This approach however has some limitations:

* It's input and output channels are referenced in every consuming flow. Each common flow must ensure unique channel names
* True encapsuation is impossible as any internal channels and components are exposed to the consuming flow. Note that these must have
unique names as well
* Since a common flow is statically bound to channels, it cannot be used in a chain without implementing some type of service-activator/
gateway wrapper.
* Since the common flow is part of the same application context (an imported resource), is not practical to configure multiple instances with
different property values, bean definitions, etc. (As of Spring 3.1 it may possible to do something with environment profiles, but would be overly complex) 

The spring-integration-flow module provides a way to implement and use common flows while addressing the above limitations. A flow is an 
abstraction of a Spring Integration component which is itself implemented with Spring Integration.  In general, a flow may expose multiple inputs and multiple outputs,
called ports. Each port binds to a channel internal to the flow. So a flow has input ports and output ports. The most common configuration is expected to define
one input port and output port. Other likely configurations are one input and zero to a small number of outputs. 

A flow may behave like a router. For example, a flow may define a primary output port for normal processing and a discard port for exceptional cases. 
Alternately, it may act like a delayer, providing no immediate response. Or it could act as an outbound channel adapter - a strictly one way or fire-and-forget flow. 

The goal is to support these, and potentially other semantics while providing better encapsulation and configuration options. Configuration is provided via properties
and or referenced bean definitions. Each flow is initialized in a child application context. This allows you to configure multiple instances of the same flow differently. 
The flow is not bound to input and output channels in the consumer context and may be naturally invoked via a flow outbound gateway. The outbound gateway may be used within 
a chain. It is also possible in theory to compose flows of other flows (NOTE: This hasn't been tested yet).    


# Usage
The flow consumer instantiates a flow and configures one or more flow outbound-gateways. 

Instantiating a flow is very simple:

	<int-flow:flow id="subflow1"/>

The above bean definition instantiates a flow defined as "subflow1". The flow id references an existing flow implementation, presumably packaged in a 
separate jar. The flow element also provides optional 'properties' attribute and a 'referenced-bean-locations' attributes to inject properties and a list of 
bean definition locations respectively. Note that any bean definition or property in the parent context may also be referenced (inherited) by the flow. As 
an alternative to the properties attribute which references a util:properties bean, A properties object may be configured as an inner bean. The following are functionally equivalent:

-----------------------------------------------------------
	
	
	<util:properties id="myprops">
    	<prop key="key1>val1</prop>
	</util:properties>

	<int-flow:flow id="subflow1" properties="myprops"/>

---------------------------------------------------------
	
	<int-flow:flow id="subflow1">
    	<props>
        	<prop key="key1>val1</prop>
    </props>
	</int-flow:flow>


If there are multiple instances of the same flow, you must specify a flow-id attribute:

	<int-flow:flow id="flow1" flow-id="subflow1">
    	<props>
        	<prop key="key1>some value</prop>
    	</props>
	</int-flow:flow>

	<int-flow:flow id="flow2" flow-id="subflow1">
    	<props>
        	<prop key="key1>another value</prop>
    	</props>
	</int-flow:flow>


By default the id attribute is used as the flow-id. 

An optional help attribute, if set to true will output the flow's description document (if there is one) to the STDOUT.


The flow is invoked via an outbound-gateway:

	<int-flow:outbound-gateway flow="flow1" input-channel="inputChannel1" output-channel="outputChannel1"/>
  
	<int-flow:outbound-gateway flow="flow2" input-channel="inputChannel2" output-channel="outputChannel2"/> 

A message sent on the gateway's input-channel is delegated to the flow. The message on the output-channel is a response from one of the flows output 
ports. The output port name is contained in the response message header 'flow.output.port' 

*NOTE: An optional input-port attribute is available if the flow defines multiple inputs, otherwise the input port it will be automatically mapped.*

Flows may also be used in a chain just as any AbstractReplyProducingMessageHandler:

	<chain input-channel="inputChannel" output-channel="outputChannel">
    	<int-flow:outbound-gateway flow="flow1"/>
    	<int-flow:outbound-gateway flow="flow2"/> 
	</chain>
	
## Referencing External Bean Definitions
Sometimes you may want to reference external bean definitions that must be defined differently for each flow instance. To do this, simply provide a list of resources containing these bean definitions.

	<int-flow:flow id="myflow" referenced-bean-locations=
		"/org/springframework/integration/flow/config/xml/ref-bean-config.xml">

 
# Implementing a Flow
The flow element is used to locate the flow's spring bean definition file(s) by convention (classpath:META-INF/spring/flows/[flow-id]/*.xml). It's bean definition 
files and any referenced-bean-locations will be used to create a child application context. The flow context must provide a FlowConfiguration bean which defines the 
flows input and output ports and maps them to internal input and output channels.

Namespace support is provided for FlowConfiguration. In the simplest case, a flow implementation encapsulates a Spring Integration flow with a single input-channel and 
a single output-channel. The required configuration is simply declared:

 	<int-flow:flow-configuration>
    	<int-flow:port-mapping input-channel="inputChannel" output-channel="outputChannel"/> 
	</int-flow:flow-configuration>

Note: The output-channel attribute is optional if the flow does not produce a response. 

For more complex scenarios, the flow configuration supports multiple port-mappings, each bound to a single input channel and 0 or more output channels. A configuration
for specifying multiple outputs looks like:

	<int-flow:flow-configuration> 
    	<int-flow:port-mapping>
        	<int-flow:input-port name="input" channel="inputChannel"/>
        	<int-flow:output-port name="output" channel="outputChannel"/>
        	<int-flow:output-port name="discard" channel="discardChannel"/>
    	</int-flow:port-mapping>
	</int-flow:flow-configuration>

If the flow defines multiple inputs, then multiple port-mapping elements must be configured. Additionally the flow client must specify the input-port in the outbound-gateway.

Flow Description File
----------------------
The user-friendly flow implementer may also create a text file classpath:META-INF/spring/flows/[flow-id]/flow.doc which describes the flow. Its contents will be written to 
STDOUT if the 'help' attribute on the client's flow declaration is set to true. 

FlowMessageHandler Internals
------------------------------
Currently, all defined outputs are automatically bridged to a PublishSubscribeChannel which acts as a single output channel for the flow. Each flow outbound-gateway instance 
is backed by a FlowMessageHandler that subscribes to the flow's common PublishSubscribeChannel. This emulates a JMS topic. Each FlowMessageHandler sends request messages to the input channel mapped to the flow's input port and forwards the response, if any to its output channel. If there are multiple FlowMessageHandlers subscribed to the flow, each receives a response from the flow. A correlation id (flow conversation id) is internally generated to correlate the response message to the request message.

If the FlowMessageHandler catches an exception, it will convert it to an ErrorMessage response. Alternately, the flow can map its errorChannel to an output port

Currently flow input and output channels must inherit from SubscribableChannel, e.g., DirectChannel or PublishSubscribe channel.
