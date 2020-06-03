#!/bin/bash

set -ev

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

if [[ "$TRAVIS_REPO_SLUG" != "$REPO" ]]; then
  echo "Skipping release: wrong repository. Expected '$REPO' but was '$TRAVIS_REPO_SLUG'."
elif [[ "$TRAVIS_PULL_REQUEST" != "false" ]]; then
  echo "Skipping release. It was pull request."
elif [[ "$TRAVIS_BRANCH" != "$BRANCH" ]]; then
  echo "Skipping release. Expected '$BRANCH' but was '$TRAVIS_BRANCH'."
elif [[ -z $VERSION ]]; then
  echo "Skipping release. Version value not found."
elif ! [[ $VERSION =~ $SEMVER_REGEX ]]; then
  echo "Skipping release. Bad version used."
else
  if [[ ${BASH_REMATCH[5]} == 'SNAPSHOT' ]]; then
    echo "Doing SNAPSHOT release..."
    ./gradlew -Dorg.gradle.internal.http.socketTimeout=300000 -Dorg.gradle.internal.http.connectionTimeout=300000 publishToSonatype
  else
    echo "Doing release..."
    ./gradlew -Dorg.gradle.internal.http.socketTimeout=300000 -Dorg.gradle.internal.http.connectionTimeout=300000 publishToSonatype closeAndReleaseRepository
  fi
  echo "Release done!"
fi
