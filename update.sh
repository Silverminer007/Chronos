git stash
git fetch
git pull
systemctl stop chronos
mvn clean package -Pproduction
cp chronos.service /etc/systemd/system/
systemctl daemon-reload
mkdir /opt/chronos
mv target/chronos-BETA.jar /opt/chronos/chronos.jar
PASSWORD=$(echo 'keyword' | sha1sum)
echo "$PASSWORD"
mkdir /etc/config
echo "spring.datasource.password = ""$PASSWORD" > /etc/config/chronos.properties
mysql -u root --execute="DROP USER 'chronos'@'localhost'"
mysql -u root --execute="FLUSH PRIVILEGES;"
mysql -u root --execute="CREATE USER 'chronos'@'localhost' IDENTIFIED BY '""$PASSWORD""';"
mysql -u root --execute="GRANT ALL ON *.* TO 'chronos'@'localhost';"
systemctl start chronos
chmod +x ./update.sh
chmod +x ./install.sh
tail -f -n 50 ../logs/spring.log