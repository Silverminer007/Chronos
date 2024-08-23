# Run per User you want chronos to run on
JAVA_VERSION=21.0.4-tem

if [ "$(whoami)" == "root" ]
then
  echo "Please don't run this script as root"
  exit
fi

# Create Directories
mkdir ~/.chronos
mkdir ~/.chronos/logs
mkdir ~/.chronos/logs/cron

# Setup Systemd to run User Units
loginctl enable-linger "$(whoami)"

# Install Java
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java $JAVA_VERSION

# Set Chronos URL
echo "Please enter the Public facing URL Chronos shall be reachable with (including http(s) and port if necessary):"
read -r url
echo chronos.base-url="$url" | tee ~/.chronos/chronos.properties
echo chronos.database-prefix="$(whoami)" >> ~/.chronos/chronos.properties

echo "Please enter the internal Port to use for Chronos. Make sure this does not conflict with any other application"
read -r port
echo "server.port=\${PORT:$port}" | tee ~/.chronos/application.properties

cp chronos.service chronos-temp.service

{
  echo WorkingDirectory="$HOME"/.chronos;
  echo ExecStart="$JAVA_HOME"/bin/java -jar "$HOME"/.chronos/chronos-frontend.jar;
  echo Environment="JAVA_HOME=$JAVA_HOME"
} >> chronos-temp.service

# Setup Chronos Service
mkdir ~/.config
mkdir ~/.config/systemd
mkdir ~/.config/systemd/user
cp chronos-temp.service ~/.config/systemd/user/chronos.service
rm chronos-temp.service
systemctl --user daemon-reload

# Setup Chronos Cronjob
line="02 * * * * $JAVA_HOME/bin/java -jar ~/.chronos/chronos-cron.jar &>~/.chronos/cron-logs"
(crontab -u "$(whoami)" -l; echo "$line" ) | crontab -u "$(whoami)" -

# Install chronos
cp ./update.sh ~/.chronos/
cd ~/.chronos || exit
chmod +x ./update.sh
./update.sh latest