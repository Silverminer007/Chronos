git stash
git fetch
git pull
mvn clean package -Pproduction
systemctl stop chronos
cp chronos.service /etc/systemd/system/
systemctl daemon-reload
mv target/chronos-ALPHA.jar ../../chronos.jar
PASSWORD=$(echo 'keyword' | sha1sum)
echo "$PASSWORD"
mkdir /etc/config
echo "spring.datasource.password = ""$PASSWORD" > /etc/config/chronos.properties
mysql -u root --execute="DROP USER 'chronos'@'localhost'"
mysql -u root --execute="FLUSH PRIVILEGES;"
mysql -u root --execute="CREATE USER 'chronos'@'localhost' IDENTIFIED BY '""$PASSWORD""';"
mysql -u root --execute="GRANT ALL ON *.* TO 'chronos'@'localhost';"
systemctl start chronos
systemctl status chronos
chmod +x ./update.sh
chmod +x ./install.sh