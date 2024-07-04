cd /opt/chronos || (echo "Updating Chronos failed" & exit)
echo Please enter your GitHub PAT
read -r pat
echo "Update Frontend START"
systemctl stop chronos
./update-frontend.sh "latest" chronos.jar "$pat"
systemctl stop chronos
echo "Update Frontend END"
echo "Update Cronjob START"
./update-cron.sh "latest" chronos-cron.jar "$pat"
echo "Update Cronjob END"