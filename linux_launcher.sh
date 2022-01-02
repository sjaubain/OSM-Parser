#!/bin/bash

echo "running application"
export JAVA_FX=/usr/share/openjfx/lib
java --module-path=$JAVA_FX --add-modules=javafx.fxml,javafx.controls,javafx.graphics,javafx.web,\
javafx.base -jar osmparser-1.0-SNAPSHOT-launcher.jar
