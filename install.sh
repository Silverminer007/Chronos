mkdir /opt/kjgtermine
cd /opt/kjgtermine
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem
apt install mysql-server
mysql -u root
CREATE DATABASE kjgtermine;
CREATE USER 'kjgtermine'@'localhost' IDENTIFIED BY '';
GRANT ALL ON *.* TO 'kjgtermine'@'localhost';
exit
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/kjgtermine.jar
useradd kjgtermine
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/kjgtermine.service
mv kjgtermine.service /etc/systemd/system/
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/update.sh
chmod +x update.sh
systemctl start kjgtermine