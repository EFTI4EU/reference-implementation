# eFTI CA APP mock 

This project is the mock for the Competent Authority application.

## Prerequisites

- NodeJs version 	^18.19, ^20.11 or ^22.0.0
- The deploy/local docker project running

For easier configuration, this mock use the borduria gate as it's associated gate, so make sure it is up and running. 
It also use the apache httpd server to serve the web app, and keycloak for authentication purpose.

Open your host file (for windows C:\Windows\System32\drivers\etc\hosts) and add the following:
```
127.0.0.1 portal.efti.fr
```

## Run the project

move to the root of this project and run

```
npm ci
npm start
```

Then go to http://portal.efti.fr:83 and you should be asked to log in:
- user: user_bo
- password: Azerty59*123
