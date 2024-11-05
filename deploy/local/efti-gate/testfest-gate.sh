REPOS_DIR="/home/install/repos/efti4eu/reference-implementation"
GATE_DIR="${REPOS_DIR}/deploy/local/efti-gate"
DOMIBUS_DIR="${REPOS_DIR}/deploy/local/domibus"

#Utils
apt update
apt install -y net-tools
apt install -y git
apt install -y wget
apt install -y unzip
apt install -y dos2unix

#Repos
mkdir -p $REPOS_DIR
git clone https://github.com/efti4eu/reference-implementation  $REPOS_DIR
cd $REPOS_DIR
git switch testfest-fr

#JAVA
apt install -y openjdk-17-jre

#RabbitMQ
apt install -y rabbitmq-server
rabbitmq-plugins enable rabbitmq_management
cp -f "${GATE_DIR}/rabbitmq/rabbitmq.conf" /etc/rabbitmq/
cp -f "${GATE_DIR}/rabbitmq/rabbitmq-defs.json" /etc/rabbitmq/definitions.json
service rabbitmq-server restart

#Postgres
apt install -y postgresql
sudo -H -u postgres createdb efti
sudo -H -u postgres psql -d efti < "${GATE_DIR}/sql/1-create_tables.sql"
sudo -H -u postgres psql -d efti < "${GATE_DIR}/sql/5-create_tables_FR.sql"
sudo -H -u postgres psql -d efti < "${GATE_DIR}/sql-meta/5-create_tables_FR.sql"

#Keycloak
wget https://github.com/keycloak/keycloak/releases/download/22.0.5/keycloak-22.0.5.tar.gz
tar -xzvf keycloak-22.0.5.tar.gz
mv keycloak-22.0.5 /opt/keycloak

/opt/keycloak/bin/kc.sh import --file "${GATE_DIR}/keycloak/fr-export.json"

cp "${GATE_DIR}/keycloak/keycloak.service" /etc/systemd/system
sudo systemctl daemon-reload
sudo systemctl enable keycloak.service
sudo systemctl start keycloak.service
