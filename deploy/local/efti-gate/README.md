# Local environment sample for the gate

The aim of this project is to provide a simple environment to make multiple gates and platforms communicate together. 

In order to demonstrate this, we create a fictional ecosystem of 3 gates and 3 platforms.
* Syldavia (platform: massivedynamic, uses eDelivery)
* Borduria (platform: acme, uses REST api)
* Listenbourg (platform: umbrellacorporation, uses eDelivery)
Each gate can communicate with its related platform as well as any other gate.

## Prerequisites 

Download
* Docker
* Postman
* This project

To avoid conflicts, this project uses a custom docker network `efti-network`. Ensure that this network is available before starting. You can create it simply by running this command:
```
docker network create efti-network
```
## Run the project 

The project includes all the required components to properly run the gates and the platforms (Postgres, RabbitMQ, Keycloak, ...).
To run the project, use the deploy script to build and deploy all services:

```shell
# Build with tests
./deploy.sh

# ...or build without tests
./deploy.sh skip-tests
```

This will launch 12 containers:
* rabbitmq
* platform-ACME
* platform-MASSIVE
* platform-UMBRELLA
* efti-gate-BO
* efti-gate-LI
* efti-gate-SY
* psql
* psql-meta
* keycloak

To display logs of a container 
```
docker logs <container name>
```

Finally, open your host file (for windows C:\Windows\System32\drivers\etc\hosts) and add the following:
```
127.0.0.1 auth.gate.borduria.eu
127.0.0.1 auth.gate.syldavia.eu
127.0.0.1 auth.gate.listenbourg.eu
```

### Send a message

Now that domibus is ready, it is time to open Postman

First, import the postman collections from `utils/postman` by using the "file > import" function

If you followed the naming convention for service account, you should not need to change anything. Otherwise, go to Authorization tab of each request and update user password

You can see a pre-configured sample message for each allowed flow between gate and gate, and gate and platform. Click "send" and you should see it in sender's and recipient's domibus.

### Send a message to the AAP interface

The Postman collection contains some messages to test th AAP interface of the Borduria gate (folder BO/AAP). 
To use the AAP Interface, some changes are required: 
- in efti-gate/gate/ENV/BO.env, change PROFILE=BO to PROFILE=BO,certAuth (this will enable TLS on the gate)
- in domibus/sh/setenv-node-1.sh, add the following lines at the end of the file: 
```
export JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStore=/opt/domibus/conf/ca_cert/root_ca.jks"
export JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStorePassword=changeit"
```
Within Postman, configure a client certificate to authenticate. Use the gate.pfx-file which is included in the gate resources and the hostname https://efti.gate.borduria.eu:8880. 
Then, use the Platform Simulator to create Test data and send the prepared messages. 
From a content point of view, they are the same as the messages which can be found in the GAte-Folder. 
Differences: 
- no Auth message to get the token 
- Authority Object included for POST UIL and Identifier
The results should be the same as for the Messages in the Gate folder. 