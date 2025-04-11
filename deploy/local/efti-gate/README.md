<h1>Local environment sample for the gate</h1>


The aim of this project is to provide a simple environment to make multiple gates and platforms communicate together. 

In order to demonstrate this, we create a fictional ecosystem of 3 gates and 3 platforms.
<ul>
<li>Syldavia (platform: massivedynamic)</li>
<li>Borduria (platform: acme)</li>
<li>Listenbourg (platform: umbrellacorporation)</li>
</ul>
<br>
Each gate can communicate with its related platform as well as any other gate.

<h3> Prerequisites </h3>

Download
<ul>
  <li>Docker</li>
  <li>Postman</li>
  <li>This project</li>
</ul>

To avoid conflicts, this project uses a custom docker network `efti-network`. Ensure that this network is available before starting. You can create it simply by running this command:
```
docker network create efti-network
```
<h3> Run the project </h3>

The project includes all the required components to properly run the gates and the platforms (Postgres, RabbitMQ, Keycloak, ...).
To run the project, use the deploy script to build and deploy all services:

```shell
# Build with tests
./deploy.sh

# ...or build without tests
./deploy.sh skip-tests
```

This will launch 12 containers:
<ul>
  <li>rabbitmq</li>
  <li>platform-ACME</li>
  <li>platform-MASSIVE</li>
  <li>platform-UMBRELLA</li>
  <li>efti-gate-BO</li>
  <li>efti-gate-LI</li>
  <li>efti-gate-SY</li>
  <li>psql</li>
  <li>psql-meta</li>
  <li>keycloak</li>
</ul>

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

<h3>Send a message</h3>

Now that domibus is ready, it is time to open Postman

First, import the postman collections from `utils/postman` by using the "file > import" function

If you followed the naming convention for service account, you should not need to change anything. Otherwise, go to Authorization tab of each request and update user password

You can see a pre-configured sample message for each allowed flow between gate and gate, and gate and platform. Click "send" and you should see it in sender's and recipient's domibus