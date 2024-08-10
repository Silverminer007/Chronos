# Versions
SIGNAL_VERSION=0.13.4
JAVA_VERSION=21.0.2-tem
MYSQL_VERSION=0.8.32-1

# Create Directories
mkdir /opt/chronos
mkdir /opt/chronos/logs
mkdir /opt/chronos/logs/cron

apt install curl
apt install zip

# Install Java
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java $JAVA_VERSION
# Install MySQL
curl -OL https://dev.mysql.com/get/mysql-apt-config_"$MYSQL_VERSION"_all.deb
apt install ./mysql-apt-config_"$MYSQL_VERSION"_all.deb
apt update
apt install mysql-server
rm mysql-apt-config_"$MYSQL_VERSION"_all.deb
# Install Signal CLI
if [ -e /usr/local/bin/signal-cli ]
then
  echo "Signal CLI is already installed, skipping"
else
  wget https://github.com/AsamK/signal-cli/releases/download/v"${SIGNAL_VERSION}"/signal-cli-"${SIGNAL_VERSION}".tar.gz
  tar xf signal-cli-"${SIGNAL_VERSION}".tar.gz -C /opt
  ln -sf /opt/signal-cli-"${SIGNAL_VERSION}"/bin/signal-cli /usr/local/bin/
  rm signal-cli-"${SIGNAL_VERSION}".tar.gz
fi
# Install jq
apt-get install jq -y

# Create Database
PASSWORD=$(echo 'keyword' | sha1sum)
mkdir /etc/config
echo "spring.datasource.password = ""$PASSWORD" | tee /etc/config/chronos.properties
mysql -u root --execute="CREATE DATABASE chronos;"
mysql -u root --execute="CREATE USER 'chronos'@'localhost' IDENTIFIED BY '""$PASSWORD""';"
mysql -u root --execute="GRANT ALL ON *.* TO 'chronos'@'localhost';"

# Set Chronos URL
echo "Please enter the URL Chronos shall be reachable with (including http(s) and port if necessary:"
read -r url
echo chronos.base-url="$url" | tee /opt/chronos/chronos.properties

# Setup Chronos Service
cp chronos.service /etc/systemd/system/
systemctl daemon-reload

# Setup Chronos Cronjob
line="02 * * * * /root/.sdkman/candidates/java/$JAVA_VERSION/bin/java -jar /opt/chronos/chronos-cron.jar &>/opt/chronos/cron-logs"
(crontab -u "$(whoami)" -l; echo "$line" ) | crontab -u "$(whoami)" -

# Install chronos
cp ./update.sh /opt/chronos/
cd /opt/chronos || exit
chmod +x ./update.sh
./update.sh