<h1>Local environment sample for Domibus</h1>

The aim of this project is to provide a simple environment to make multiple domibus access points communicate together. Each domibus is multi tenant and/or multi instance

In order to demonstrate this, we create a fictional ecosystem of 3 gates and 3 platforms.
<ul>
<li>Syldavia (platform: massivedynamic)</li>
<li>Borduria (platform: acme)</li>
<li>Listenbourg (platform: umbrellacorporation)</li>
</ul>
<br>
Each gate can communicate with its related platform as well as any other gate.
Syldavia and Borduria share the same domibus (named <b>sybo</b> below), Listenbourg has its own domibus (named <b>li</b>) , and the 3 platforms share the same domibus.

This sample is based on domibus quick start guide and domibus test guide, for more detail see https://ec.europa.eu/digital-building-blocks/wikis/display/DIGITAL/Domibus

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

This project uses the Domibus docker image maintained by the efti4eu project. The project ensures that all the MariaDB databases and the related scripts have been successfully done before statiring Domibus instances.
To run the project, use the following command:
```
docker compose up -d
```
this will launch 12 containers:
<ul>
  <li>activemq-li</li>
  <li>activemq-sybo</li>
  <li>activemq-platform</li>
  <li>mariadb-li</li>
  <li>mariadb-sybo</li>
  <li>mariadb-platform</li>
  <li>domibus-li-1</li>
  <li>domibus-li-2</li>
  <li>dominus-sybo-1</li>
  <li>dominus-sybo-2</li>
  <li>dominus-platform</li>
  <li>nginx</li>
</ul>

Once everything has correctly started, you should be able to access each domibus login page: (open them in different browsers)
<ul>
  <li>domibus sybo : http://localhost:8081/domibus/ </li>
  <li>domibus li : http://localhost:8090/domibus/ </li>
  <li>domibus platform : http://localhost:8100/domibus/ </li>
</ul>

You can now login to each domibus. The login is <b>super</b>, and the password is generated each time, so look in the log for something like this:
```
> 2023-10-27T09:44:18.840284175Z 2023-10-27 09:44:18,838 [edelivery@172.19.0.4] [] [] [] [main]  INFO e.d.c.u.u.UserManagementServiceImpl:335 - Default password for user [super] is [eba6db02-1648-4683-aef6-52652b460226].
```
To display logs of a container 
```
docker logs <container name>
```
After first login, you will be asked to update your password.

Once logged, we need to setup each domain, you can switch domains from the list at the top right corner.
For each domains you need to:

- Add the related pmode file. To do this, click on PMode menu on the left, then current > upload and pass it the domain-pmode.xml file stored in the pmodes folder of this project.
You should see a message telling you the upload is ok.
- In menu Plugins Users (and not Users!) , create a user that will be used as a service account to authenticate the requests. To be compatible with the postman, name it <b>'domain_service_account'</b> (ex: <b>syldavia_service_account</b>) with password <b>Azerty59*1234567</b> and role <b>admin</b>. After creating the user you need to click 'save' on the bottom or it will not work

Finally, open your host file (for windows C:\Windows\System32\drivers\etc\hosts) and add the following:
```
127.0.0.1 efti.gate.syldavia.eu
127.0.0.1 efti.gate.borduria.eu
127.0.0.1 efti.gate.listenbourg.eu
127.0.0.1 efti.platform.massivedynamic.com
127.0.0.1 efti.platform.acme.com
127.0.0.1 efti.platform.umbrellainc.com
```

<h3>Send a message</h3>

Now that domibus is ready, it is time to open Postman

First, import the postman collections from `utils/postman` by using the "file > import" function

If you followed the naming convention for service account, you should not need to change anything. Otherwise, go to Authorization tab of each request and update user password

You can see a pre-configured sample message for each allowed flow between gate and gate, and gate and platform. Clic "send" and you should see it in sender's and recipient's domibus
