mkdir /opt/kjgtermine
cd /opt/kjgtermine || echo "Installation failed" | exit
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
apt install mysql-server
PASSWORD=$('keyword' | sha1sum)
echo "spring.datasource.password = " > /etc/config/kjgtermine.properties
$PASSWORD >> /etc/config/kjgtermine.properties
mysql -u root
CREATE DATABASE kjgtermine;
CREATE USER 'kjgtermine'@'localhost' IDENTIFIED BY "$PASSWORD";
GRANT ALL ON *.* TO 'kjgtermine'@'localhost';
exit
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/kjgtermine.jar
useradd kjgtermine
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/kjgtermine.service
mv kjgtermine.service /etc/systemd/system/
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/update.sh
chmod +x update.sh
systemctl start kjgtermine