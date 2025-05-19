# Local environment sample for Domibus

The aim of this project is to provide a simple environment to make multiple Domibus access points communicate together. Each Domibus is multi tenant and/or multi instance

In order to demonstrate this, we create a fictional ecosystem of 3 gates and 3 platforms.
* Syldavia (platform: massivedynamic)
* Borduria (platform: acme)
* Listenbourg (platform: umbrellacorporation)

* Each gate can communicate with its related platform as well as any other gate. Syldavia and Borduria share the same Domibus (named __sybo__ below), Listenbourg has its own Domibus (named __li__) , and the 3 platforms share the same Domibus.

This sample is based on Domibus quick start guide and Domibus test guide, for more detail see https://ec.europa.eu/digital-building-blocks/wikis/display/DIGITAL/Domibus

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

This project uses the Domibus docker image maintained by the eFTI4EU project. The project ensures that all the MariaDB databases and the related scripts were successfully done before starting Domibus instances.
To run the project, use the following command:

```
docker compose up -d
```

this will launch 12 containers:
* activemq-li
* activemq-sybo
* activemq-platform
* mariadb-li
* mariadb-sybo
* mariadb-platform
* domibus-li-1
* domibus-li-2
* domibus-sybo-1
* domibus-sybo-2
* domibus-platform
* nginx

Once everything has correctly started, you should be able to access each Domibus login page: (open them in different browsers)
* Domibus sybo : http://localhost:8081/domibus/ 
* Domibus li : http://localhost:8090/domibus/ 
* Domibus platform : http://localhost:8100/domibus/ 

You can now login to each Domibus. The login is `super`, and the password is generated each time, so look in the log for something like this:

```
> 2023-10-27T09:44:18.840284175Z 2023-10-27 09:44:18,838 [edelivery@172.19.0.4] [] [] [] [main]  INFO e.d.c.u.u.UserManagementServiceImpl:335 - Default password for user [super] is [eba6db02-1648-4683-aef6-52652b460226].
```

To display logs of a container

```
docker logs <container name>
```

After first login, you will be asked to update your password.

Once logged, we need to setup each domain. you can switch domains from the list at the top right corner.
For each domain you need to:

- Add the related pmode file. To do this, click on PMode menu on the left, then current > upload and pass it the domain-pmode.xml file stored in the pmodes folder of this project.
  You should see a message telling you the upload is ok.
- In menu Plugins Users (and not Users!) , create a user that will be used as a service account to authenticate the
  requests. To be compatible with the postman, name it `domain_service_account` (ex: `syldavia_service_account`) with password `Azerty59*1234567` and role `admin`. After creating the user, you need to click 'save' on the bottom or it will not work

Finally, open your host file (for windows C:\Windows\System32\drivers\etc\hosts) and add the following:

```
127.0.0.1 efti.gate.syldavia.eu
127.0.0.1 efti.gate.borduria.eu
127.0.0.1 efti.gate.listenbourg.eu
127.0.0.1 efti.platform.massivedynamic.com
127.0.0.1 efti.platform.acme.com
127.0.0.1 efti.platform.umbrellainc.com
```

## Send a message

Now that Domibus is ready, it is time to open Postman

First, import the postman collections from `utils/postman` by using the "file > import" function

If you followed the naming convention for the service account, you should not need to change anything. Otherwise, go to Authorization tab of each request and update the user password

You can see a pre-configured sample message for each allowed flow between gate and gate, and gate and platform. Click "send" and you should see it in sender's and recipient's Domibus
