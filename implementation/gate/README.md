# Gate core

This is the main component. It implements the whole process of the gate including interfaces, authentication, request 
handling, external systems connections, workflow definitions, ... It is the entry point of the gate for all external 
systems. For some processes, it can interact directly with the other parts of the systems, for some other ones, it 
passes through other components.

## Local development

Start the either via:
* [local docker compose stack](../../deploy/local/efti-gate/README.md) 
* standalone Java app running on host

### Standalone Java app running on host

To launch gate instance "BO" in your IDE, create a run configuration with:
* type: Spring Boot
* main class: `eu.efti.eftigate.EftiGateApplication`
* JVM args: `-Dspring.profiles.active=BO`
