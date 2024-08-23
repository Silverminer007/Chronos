REPO="Silverminer007/Chronos"
VERSION=$1
GITHUB="https://api.github.com"

alias errcho='>&2 echo'

function gh_curl {
  curl -H "Authorization: token $TOKEN" \
       -H "Accept: application/vnd.github.v3.raw" \
       "$@"
}

function downloadAssets {
    FILE=$1
    if [ "$VERSION" = "latest" ]; then
      # Github should return the latest release first.
      parser=".[0].assets | map(select(.name == \"$FILE\"))[0].id"
    else
      parser=". | map(select(.tag_name == \"$VERSION\"))[0].assets | map(select(.name == \"$FILE\"))[0].id"
    fi;

    asset_id=$(gh_curl -s $GITHUB/repos/$REPO/releases | jq "$parser")
    if [ "$asset_id" = "null" ]; then
      echo "ERROR: version not found $VERSION"
      exit 1
    fi;

    wget -q --auth-no-challenge --header='Accept:application/octet-stream' \
      https://"$TOKEN":@api.github.com/repos/$REPO/releases/assets/"$asset_id" \
      -O "$FILE"
}

cd ~/.chronos || (echo "Updating Chronos failed" & exit)
echo Please enter your GitHub PAT
read -r TOKEN
echo "Update Frontend START"
systemctl --user stop chronos
downloadAssets chronos-frontend.jar
systemctl --user stop chronos
echo "Update Frontend END"
echo "Update Cronjob START"
downloadAssets chronos-cron.jar
echo "Update Cronjob END"