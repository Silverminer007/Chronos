systemctl stop kjgtermine
sdk install java 21.0.2-tem
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/kjgtermine.jar
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/kjgtermine.service
mv kjgtermine.service /etc/systemd/system/
curl -OL https://github.com/Silverminer007/Gruppentool/releases/latest/update.sh
chmod +x update.sh
systemctl start kjgtermine