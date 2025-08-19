#!/bin/bash

set -e

# https://github.com/fsaintjacques/semver-tool/blob/master/src/semver
NAT='0|[1-9][0-9]*'
ALPHANUM='[0-9]*[A-Za-z-][0-9A-Za-z-]*'
IDENT="$NAT|$ALPHANUM"
FIELD='[0-9A-Za-z-]+'
SEMVER_REGEX="\
^($NAT)\\.($NAT)\\.($NAT)\
(\\-(${IDENT})(\\.(${IDENT}))*)?\
(\\+${FIELD}(\\.${FIELD})*)?$"


REPO="rollbar/rollbar-java"
BRANCH="master"
VERSION=`cat gradle.properties | awk -F= '/^VERSION_NAME/ { print $2 }'`

IS_PULL_REQUEST=false
# According to GH docs, this is only set for PRs. There doesn't seem to be a specific variable
# to indicate a job is running due to a PR
# See https://docs.github.com/en/actions/reference/environment-variables
if [[ "$GITHUB_BASE_REF" != "" ]]; then
  IS_PULL_REQUEST=true
fi

EXPECTED_REF="refs/heads/${BRANCH}"

if [[ "$GITHUB_REPOSITORY" != "$REPO" ]]; then
  echo "Skipping release: wrong repository. Expected '$REPO' but was '$GITHUB_REPOSITORY'."
elif [[ "$IS_PULL_REQUEST" != "false" ]]; then
  echo "Skipping release. It was pull request."
elif [[ "$GITHUB_REF" != "$EXPECTED_REF" ]]; then
  echo "Skipping release. Expected '$EXPECTED_REF' but was '$GITHUB_REF'."
elif [[ -z $VERSION ]]; then
  echo "Skipping release. Version value not found."
elif ! [[ $VERSION =~ $SEMVER_REGEX ]]; then
  echo "Skipping release. Bad version used."
else
  echo "Doing release with Vanniktech Maven Publish plugin..."
  ./gradlew -Dorg.gradle.internal.http.socketTimeout=300000 \
            -Dorg.gradle.internal.http.connectionTimeout=300000 \
            publish
  echo "Release done!"
fi
