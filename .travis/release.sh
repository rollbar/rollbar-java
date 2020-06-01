#!/bin/bash

REPO="rollbar/rollbar-java"
BRANCH="master"

set -ev

if [[ "$TRAVIS_REPO_SLUG" != "$REPO" ]]; then
  echo "Skipping release: wrong repository. Expected '$REPO' but was '$TRAVIS_REPO_SLUG'."
elif [[ "$TRAVIS_PULL_REQUEST" != "false" ]]; then
  echo "Skipping release. It was pull request."
elif [[ "$TRAVIS_BRANCH" != "$BRANCH" ]]; then
  echo "Skipping release. Expected '$BRANCH' but was '$TRAVIS_BRANCH'."
else
 echo "Doing release..."
  ./gradlew -Dorg.gradle.internal.http.socketTimeout=300000 -Dorg.gradle.internal.http.connectionTimeout=300000 publish
  echo "Release done!"
fi
