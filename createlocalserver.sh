#!/bin/bash
git="https://aerialworks.ddns.net/ASF/ConnectiveStandalone.git"
dir="$(pwd)"

echo 'Updating standalone installation for testing...'

echo Cloning git repository...
tmpdir="/tmp/build-rats-connective-http-standalone/$(date "+%s-%N")"
rm -rf "$tmpdir"
mkdir -p "$tmpdir"
git clone $git "$tmpdir"
cd "$tmpdir"
echo

function exitmeth() {
    cd "$dir"
    rm -rf "$tmpdir"
    echo
    exit $1
}

function execute() {
    gradle installation || return $?
    if [ ! -d "$dir/server" ]; then
        mkdir "$dir/server"
    fi
    cp -rf "build/Installations/." "$dir/server"
    cp -rf "$dir/server/libs" "$dir/libraries"
}

echo Building...
execute
exitmeth $?
