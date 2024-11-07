REPOS_DIR="/home/install/repos/efti4eu/reference-implementation"
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

#ActiveMQ
wget http://archive.apache.org/dist/activemq/5.16.3/apache-activemq-5.16.3-bin.tar.gz
tar -xvzf apache-activemq-5.16.3-bin.tar.gz
mkdir /opt/activemq
mv apache-activemq-5.16.3/* /opt/activemq
addgroup --quiet --system activemq
adduser --quiet --system --ingroup activemq --no-create-home --disabled-password activemq
chown -R activemq:activemq /opt/activemq
sudo /opt/activemq/bin/activemq start

#MariaDB
curl -LsS https://r.mariadb.com/downloads/mariadb_repo_setup | sudo bash
apt install mariadb-server mariadb-client -y
service mariadb start
mysql -u root -proot -e "CREATE DATABASE DB"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/01-init.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/02-mysql-5.1-multi-tenancy.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/03-mysql-5.1-multi-tenancy-data.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/04-init-france.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/05-mysql-5.1-france.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/06-mysql-5.1-data-france.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/07-init-ttf.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/08-mysql-5.1-ttf.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/09-mysql-5.1-data-ttf.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/10-init-default.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/11-mysql-5.1-default.sql"
mysql -u root -proot < "${DOMIBUS_DIR}/mariadb/fr-ttf/12-mysql-5.1-data-default.sql"

#Domibus
wget https://ec.europa.eu/digital-building-blocks/artifact/repository/eDelivery/eu/domibus/domibus-msh-distribution/5.1/domibus-msh-distribution-5.1-tomcat-full.zip
unzip -o domibus-msh-distribution-5.1-tomcat-full
mv domibus/ /opt/domibus/

wget https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar
mv mysql-connector-j-8.0.33.jar /opt/domibus/lib/

mv ${DOMIBUS_DIR}/domibus/fr-ttf/domains/france /opt/domibus/conf/domibus/domains/france
mv ${DOMIBUS_DIR}/domibus/fr-ttf/domains/ttf /opt/domibus/conf/domibus/domains/ttf
mv ${DOMIBUS_DIR}/domibus/fr-ttf/domibus-france.properties /opt/domibus/conf/domibus/domibus.properties
mv ${DOMIBUS_DIR}/domibus/sh/setenv-node-1.sh /opt/domibus/bin/setenv.sh
mv ${DOMIBUS_DIR}/domibus/fr-ttf/plugins-conf/france /opt/domibus/conf/domibus/plugins/config/domains/france
mv ${DOMIBUS_DIR}/domibus/fr-ttf/plugins-conf/ttf /opt/domibus/conf/domibus/plugins/config/domains/ttf
dos2unix /opt/domibus/bin/*.sh
sudo chmod 777 /opt/domibus/bin/*
#before startup, add line below to /opt/domibus/bin/setenv.sh
# export JAVA_OPTS="$JAVA_OPTS --add-exports java.base/sun.security.util=ALL-UNNAMED --add-exports java.base/sun.security.x509=ALL-UNNAMED"
sudo sh /opt/domibus/bin/startup.sh


