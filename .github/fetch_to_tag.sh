#!/usr/bin/env bash

set -e

# Github actions will do shallow clones which is great for performance, but some of our
# tasks require a history going back to the most recent tag, so this script achieves
# that, without pulling the full history

function fetch_to_tag() {
    n="$1"
    if [ "$n" == "" ]; then
        n=2
    fi
    
    echo "Fetching commits with depth ${n}"

    rev=$(git rev-list -n 1 HEAD)

    git fetch origin --depth "$n" "$rev"

    if ! git describe --tags --abbrev=0 HEAD^ 1>/dev/null 2>&1; then
        fetch_to_tag "$(expr "$n" \* 2)"
    fi
}

git fetch origin 'refs/tags/*:refs/tags/*'

fetch_to_tag
