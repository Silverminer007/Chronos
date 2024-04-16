systemctl stop kjgtermine
GITHUB_PAT=$(<../../github.pat)
git stash
git pull "https://""$GITHUB_PAT""@github.com/Silverminer007/Gruppentool.git"
mvn clean package -Pproduction
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