git stash
git fetch
git pull
mvn clean package -Pproduction
systemctl stop kjgtermine
cp kjgtermine.service /etc/systemd/system/
systemctl daemon-reload
mv target/kjgtermine-ALPHA.jar ../../kjgtermine.jar
PASSWORD=$(echo 'keyword' | sha1sum)
echo "$PASSWORD"
mkdir /etc/config
echo "spring.datasource.password = ""$PASSWORD" > /etc/config/kjgtermine.properties
mysql -u root --execute="DROP USER 'kjgtermine'@'localhost'"
mysql -u root --execute="FLUSH PRIVILEGES;"
mysql -u root --execute="CREATE USER 'kjgtermine'@'localhost' IDENTIFIED BY '""$PASSWORD""';"
mysql -u root --execute="GRANT ALL ON *.* TO 'kjgtermine'@'localhost';"
systemctl start kjgtermine
systemctl status kjgtermine
chmod +x ./update.sh
chmod +x ./install.sh