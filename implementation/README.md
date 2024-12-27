# Implementation

The development of the gate involves different components. All the components are designed as independent libraries that will be integrated in a single microservice. This was due to the simplicity of the gate workflows in a side, and the fact that the gate already uses many other components (databases, logger, brokers, ...), in other side.

However, these components are sufficiently independent to be easily used as separate microservices. An adaptation is thought required but not to change the hole architecture. Also, a set of dependencies are required, including databases, message brokers, authentication server, etc.

In addition to gate components, a simulator was developed as a mockup of external gates and platforms. This simulator is required for integration and performance tests.

Here a summary of developed components. More detailed description can be found inside each component.

![Components diagram](./components.png "Component diagram of the gate and its dependencies")

## Gate components

All the gate components are separate libraries and used to build the gate microservice.

- [Gate core](gate/README.md): This the main component. it implements the whole process of the gate including interfaces, authentication, request handling, external systems connections, workflow definitions, ... It is the entry point of the gate for all external systems. For some processes, it can interact directly with the other parts of the systems, for some other ones, it passes through other components.

- [Registry of identifiers](registry-of-identifiers/README.md): handles the access to the identifiers. This might be related to the upload of identifier information by the platforms, or to the access to this information by the competent authorities.

- [eDelivery connector](edelivery-ap-connector/README.md): Manage the communication between the gate and the related eDelivery access point (provided by Domibus)

- [Common library](commons/README.md): Includes a set of useful features

- [Logger](efti-logger/README.md): Traces different types of information including technical logs, audit trail, statistics, ...

## Simulators

The reference implementation offers two simulators to run and test the gate. The purpose of these simulators covers both development and testing. The idea is to mockup the external systems the gate might send to request to: national platforms and foreign gates.

The two simulators are available in the same microservice, but they require some dependencies. more detailed description is available [here](./platform-gate-simulator/README.md)

## Dependencies

The dependencies described in the following are external tools which are required to deploy and run the gate, but not required to build it.

- **Postgresql:** the main database of the gate. Postgresql is a widely used relational database system. It is used both for long term persistent data (mainly for the registry of identifiers), but also to manage the request lifecycle and other useful purposes.
- **RabbitMQ:** a message broker to handle asynchronous requests. RabbitMQ is a widely used message broker based on MQTP protocol (among others). All the asynchronous requests all placed in an appropriate queue inside RabbitMQ before being processed.
- **Keycloak:** an authentication server. Keycloak is used as an example of real authentication server based on OIDC/Oauth2 for authentication, identification and authorization purposes. This is not intended to be used in production; member states should adapt this reference implementation to use real authentication systems provided by the underlying authorities.
- **Domibus**: eDelivery access point. Domibus is an eDelivery (AS4) access point implementation provided by the european commission. it comes with its tow own dependencies: MariaDB as mandatory database system, and ActiveMQ as a message broker required when Domibus is deployed with more than one instance.

## Interfaces

The gate offers three interfaces: 

- eDelivery API for the foreign gates
- eDelivery and REST APIs for the local platforms
- REST API for the competent authorities

Foreign gates, as well as authorities, can search for identifiers or request datasets. The local platforms can upload identifiers information. The gate itself can search identifiers or request datasets within the foreign gates and request datasets to the local platforms. Consequently, the interface gate to gate and gate to platform are in server to server mode, while the interface authorities to gate are in client server mode.

Basically, request are always initiated either by the authorities for search or data, or by the platforms to upload identifiers. the communication between the different gate is only to forward the requests from authorities when the gate does not have locally the information.

The communication with gate is a mix of synchronous and asynchronous requests. eDelivery interfaces are always asynchronous due the architecture of the protocol.

Authority to gate interface is also asynchronous due the long time of the requests. As REST is based on http, which is a synchronous protocol, the asynchronism is handled by a pool mechanism where the authority obtains a request id immediately after sending its request and then checks periodically if a response available. These requests are pushed to RabbitMQ and processed as soon as a worker is available.

The REST interface for platforms is synchronous due to the underlying protocol and the simplicity of the requests.

The specification of different interfaces as well as the data model of different request is available in `schema` directory.

## Behavior

Here a set of sequence diagrams illustrating the workflows of different use cases.

### Upload identifiers

```mermaid
sequenceDiagram
    autonumber
    participant pfm as Platform
    participant pdom as eDelivery - Platform
    participant ldom as eDelivery - Gate
    participant lgate as Gate

    pfm->>+pdom: upload metadata
    activate pfm
    deactivate pfm
    pdom->>+ldom: forward request
    ldom->>+lgate: forward request
    lgate->>lgate: create metadata entry
    lgate->>ldom: metadataId
    deactivate lgate
    ldom->>-pdom: forward metadataId
    pdom->>-pfm: forward metadataId
    activate pfm
    pfm->>pfm: save metadataId
    deactivate pfm
```

### Search for identifiers

```mermaid
sequenceDiagram
    autonumber
    participant app as Control App
    participant gate as Local eFTI Gate
    participant mq as RabbitMQ
    participant db as dataBase Server

    app->>+gate: searchById
    gate->>db: insert(localSearch)
    gate->>mq: addMassage(localSearch)
    loop nextGate
        gate->>db: insert(remoteSearch)
        gate->>mq: addMassage(remoteSearch)
    end
    gate->>-app: reqestID

    loop statusPending
        app->>+gate: getDataByRequestId
        gate->>+db: getRequestStatus
        db->>-gate: response
        alt statusPending
            gate->>app: statusPending
        else
            gate->>-app: response
        end
    end
```

### Request dataset

```mermaid
sequenceDiagram
    autonumber
    participant app as Controler App
    participant gate as Local eFTI Gate
    participant mq as RabbitMQ
    participant db as dataBase Server

    app->>+gate: requestDataSet
    gate->>db: insertRequest
    gate->>mq: addMassage
    gate->>-app: reqestID

    loop statusPending
        app->>+gate: getDataByRequestId
        gate->>+db: getRequest
        db->>-gate: request
        alt statusPending
            gate->>app: statusPending
        else
            gate->>-app: data
        end
    end
```

### Request handling

```mermaid
sequenceDiagram
    autonumber
    participant rqh as Request Handler
    participant rph as Response Endpoint
    participant mq as RabbitMQ
    participant db as Postgres
    participant ldom as Local Domibus
    participant pdom as Platform eDelivery
    participant rdom as Remote Gate <br> eDelivery

    loop infinite
        rqh->>+mq: getTask
        mq->>-rqh: taskId
        rqh->>+db: getTaskDetails
        db->>-rqh: task
        rqh->>db: updateTaskStatus

        opt searchRequest
            alt localSearch
                rqh->>+db: searchById
                db->>-rqh: result
                rqh->>db: updateTaskStatus
            else
                rqh->>+ldom: searchById
                ldom->>+rdom: forward request
                rdom->>-ldom: response
                ldom->>-rph: forward response
            end
        end
        
        opt getDatasetRequest
            alt localPlatform
                rqh->>+ldom: getDataset
                ldom->>+pdom: forward request
                pdom->>-ldom: response
                ldom->>-rph: forward response
            else
                rqh->>+ldom: getDataset
                ldom->>+rdom: forward request
                rdom->>-ldom: response
                ldom->>-rph: forward response
            end 
        end

        rph->>db: updateTaskStatus
       
    end
```
