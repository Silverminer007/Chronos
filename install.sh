curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
curl -OL https://dev.mysql.com/get/mysql-apt-config_0.8.29-1_all.deb
apt install ./mysql-apt-config_0.8.29-1_all.deb
apt install mysql-server
PASSWORD=$('keyword' | sha1sum)
mkdir /etc/config
echo "spring.datasource.password = " > /etc/config/kjgtermine.properties
$PASSWORD >> /etc/config/kjgtermine.properties
# shellcheck disable=SC2027
mysql -u root --execute="CREATE DATABASE kjgtermine; CREATE USER 'kjgtermine'@'localhost' IDENTIFIED BY ""$PASSWORD"";GRANT ALL ON *.* TO 'kjgtermine'@'localhost';"