# Implementation

The development of the gate involves different components. All the components are designed as independent libraries that will be integrated in a single microservice. This was due to the simplicity of the gate workflows in a side, and the fact that the gate already uses many other components (databases, logger, brokers, ...), in other side.

However, these components are suffisently independent to be easyly used as separate microservices. An adaptation is thought required but not to change the hole architecture.

In addition to gate components, a simulator was developped as a mockup of external gates and platforms. This simulator is required for integration and performance tests.

Here a summary of deveoped components. More detailed description can be found inside each component.

## Gate components

These are the component constituting the gate. All of them are a separate library and used to build the gate microservice.

- [Gate core](gate/README.md): This the main component. it implements the whole process of the gate including interfaces, authentication, request handling, external systems connections, workflow definitions, ... It is the entry point of the gate for all external systems. For some processes, it can interact directly with the other parts of the systems, for some other ones, it passes through other components.

- [Registry of identifiers](registry-of-identifiers/README.md): handles the access to the identifiers. This might be related to the upload of identifier information by the platforms, or to the access to this information by the competent authorities.

- [eDelivery connector](edelivery-ap-connector/README.md): Manage the communication between the gate and the related eDelivery access point (provided by Domibus)

- [Common library](commons/README.md): Inclueds a set of usefull features

- [Logger](efti-logger/README.md): Traces different types of information including technical logs, audit trail, statistics, ...

## Simulators
