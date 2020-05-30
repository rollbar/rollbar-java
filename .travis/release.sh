#!/bin/bash

set -ev

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
else
  if [[ $VERSION == *"SNAPSHOT"* ]]; then
     echo "Doing SNAPSHOT release..."
     ./gradlew -Dorg.gradle.internal.http.socketTimeout=300000 -Dorg.gradle.internal.http.connectionTimeout=300000 publishToSonatype
  else
     echo "Doing release..."
     ./gradlew -Dorg.gradle.internal.http.socketTimeout=300000 -Dorg.gradle.internal.http.connectionTimeout=300000 publishToSonatype closeAndReleaseRepository
  fi
  echo "Release done!"
fi
