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

You can see a pre-configured sample message for each allowed flow between gate and gate, and gate and platform. Click "send" and you should see it in sender's and recipient's domibus