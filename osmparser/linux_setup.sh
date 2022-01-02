#!/bin/bash

echo "updating apt"
apt update 

echo "checking java sdk"
# can be replaced by default-jdk
apt install openjdk-11-jdk

echo "installing FX components"
apt install openjfx

echo "-----BUILD------"
echo "fetching maven tools"
apt install maven

echo "building project with maven"
mvn -f pom_linux.xml clean package

echo "fetching jar file"
mv ./target/osmparser-1.0-SNAPSHOT-launcher.jar .

echo "------END-------"