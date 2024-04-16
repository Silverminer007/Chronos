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
mysql -u root --execute="ALTER USER 'kjgtermine'@'localhost' IDENTIFIED BY '""$PASSWORD""';"
systemctl start kjgtermine