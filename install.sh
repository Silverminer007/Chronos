curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
curl -OL https://dev.mysql.com/get/mysql-apt-config_0.8.29-1_all.deb
apt install ./mysql-apt-config_0.8.29-1_all.deb
apt install mysql-server
mkdir /opt/chronos/logs
sudo echo "chronos.base-url=http://localhost:8080" | sudo tee /opt/chronos/chronos.properties
PASSWORD=$(echo 'keyword' | sha1sum)
sudo mkdir /etc/config
sudo echo "spring.datasource.password = ""$PASSWORD" | sudo tee /etc/config/chronos.properties
# shellcheck disable=SC2027
mysql -u root --execute="CREATE DATABASE chronos;"
mysql -u root --execute="CREATE USER 'chronos'@'localhost' IDENTIFIED BY '""$PASSWORD""';"
mysql -u root --execute="GRANT ALL ON *.* TO 'chronos'@'localhost';"
export VERSION=0.13.4
wget https://github.com/AsamK/signal-cli/releases/download/v"${VERSION}"/signal-cli-"${VERSION}".tar.gz
sudo tar xf signal-cli-"${VERSION}".tar.gz -C /opt
sudo ln -sf /opt/signal-cli-"${VERSION}"/bin/signal-cli /usr/local/bin/