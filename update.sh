systemctl stop kjgtermine
GITHUB_PAT=$(<github.pat)
cd ..
git clone https://"$GITHUB_PAT"@github.com/Silverminer007/Gruppentool.git
cd Gruppentool || echo "Update failed" | exit
mvn clean package -Pproduction
cp kjgtermine.service /etc/systemd/system/
systemctl deamon-reload
mv target/kjgtermine-ALPHA.jar ../../kjgtermine.jar
PASSWORD=$('keyword' | sha1sum)
mkdir /etc/config
echo "spring.datasource.password = " > /etc/config/kjgtermine.properties
$PASSWORD >> /etc/config/kjgtermine.properties
mysql -u root --execute="ALTER USER 'kjgtermine'@'localhost' IDENTIFIED BY ""$PASSWORD"";"
systemctl start kjgtermine