mkdir /opt/kjgtermine
cd /opt/kjgtermine || echo "Installation failed" | exit
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
apt install mysql-server
PASSWORD=$('keyword' | sha1sum)
mkdir /etc/config
echo "spring.datasource.password = " > /etc/config/kjgtermine.properties
$PASSWORD >> /etc/config/kjgtermine.properties
# shellcheck disable=SC2027
mysql -u root --execute="CREATE DATABASE kjgtermine; CREATE USER 'kjgtermine'@'localhost' IDENTIFIED BY ""$PASSWORD"";GRANT ALL ON *.* TO 'kjgtermine'@'localhost';"
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/download/kjgtermine.jar
useradd kjgtermine
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/download/kjgtermine.service
mv kjgtermine.service /etc/systemd/system/
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/download/update.sh
chmod +x update.sh
systemctl start kjgtermine