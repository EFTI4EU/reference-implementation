# eFTI4EU Reference Implementation

This project contains the code base of the reference implementation for the eFTI4EU regulation. The project is still in progress and no release has been issued yet. Once released, involved member states can use it as a base code, base architecture or test reference for their national implementation.

## Content of the reference implementation

The reference implementation contains the minimal code base to run and test the eFTI gate in conformance with the eFTI4EU regulation. It is developed and maintained by different member states involved in this project.

The reference implementation shall not be used in production as such. It is the responsibility of each member state to adapt it to their national requirements.

### What the Reference Implementation is

- One of efti4eu project deliverables​
- Conformant the latest version of regulation​
- Open source​
- Functional implementation with shared code​

### What the reference implementation is not

- A version which contains additional modifications or changes proposed by the project​
- Ready for production use​
- A national implementation​

### How the reference implementation might be used

- As a base code for national implementations: member states can start from the reference implementation and adapt it to their requirements, constraints and environment in order to build a national implementation
- As reference architecture: member states can be inspired bu the reference implementation design and technical choice for their national implementations
- Aa test reference: if a national implementation works correctly with the reference implementation, this means that it should work correctly with the other national gates as well

Member states should not put the reference implementation as it is to production. In fact, many design and technical choices have been taken during the development of the reference implementation following general environment purpose and best practices. These choices might not necessarily be suitable for all use cases, and member states shall look deeply to the design and the implementation before going to production. Also, the available authentication model, even based on OpenID standard, is very limited and member states shall adapt it to their respective authentication systems.

## Organization of the repository

Besides the code base, the repository contains some other useful content. The repository is organized as follows:

- `implementation`: contains the code base of microservices, libraries and other tools
- `deploy`: contains the required elements to run and test the code base
- `schema`: contains different data models and interface definitions
- `utils`: contains some useful content such as a set of postman collections to test the gate

## Further documentation

More detailed documentation of different elements of this repository can be found at their respective locations. Here a summary:

- [Implementation](implementation/README.md)
    - [Gate core](implementation/gate/README.md)
    - [Registry of identifiers](implementation/registry-of-identifiers/README.md)
    - [eDelivery access point connector](implementation/edelivery-ap-connector/README.md)
    - [Common library](implementation/commons/README.md)
    - [Logger](implementation/efti-logger/README.md)
    - [Platform and gate simulator](implementation/platform-gate-simulator/README.md)
- [Schemas](schema/README.md)
    - [Data models](schema/xsd/README.md)
    - [API definitions](schema/api-schemas/README.md)
- [Deployment environments](deploy/README.md)
    - [Local Deployment environments](deploy/local/README.md)
        - [Gate](deploy/local/efti-gate/README.md)
        - [Domibus](deploy/local/domibus/README.md)
- [Utils](utils/README.md)
