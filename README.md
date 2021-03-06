# OSM Parser - Graph builder

<p align="center">
  <img src="images/screenshot.png" style="display: block; margin: 0 auto" />
</p>

## Overview

OSM Parser is a Java application that aims to allow the user to parse data from OpenStreetMap ([https://www.openstreetmap.org/]()), in particular the roads and cities and turn them into a graph, perform opertions on it such as computing shortest paths, and export it as a CSV file, PNG or EPS image. The GUI uses the JavaFX library and *osmosis* to filter data from large .pbf files, (You can find them [here](https://download.geofabrik.de/)). Osmosis is a command line tool that lets you keep nodes or ways with specific tags, reject specific ones with other tags, choose elements according to some desired specification, fetch elements from a database,..

The documentation and source code for osmosis can be found here :
* github [source code](https://github.com/openstreetmap/osmosis).
* wiki [detailed specifications](https://wiki.openstreetmap.org/wiki/Osmosis/Detailed_Usage_0.48).

## Status

Work still in progress. Please report all bugs and suggestion.

## Installation

```bash
git clone https://github.com/sjaubain/OSM-Parser.git
```

### Requirements
* Java set up on your machine
* Osmosis installed (follow instruction on official site)
For Linux, enter following command :

```bash
sudo apt install osmosis
```

Since JavaFX components don't come with Java development kit after version 8, it is strongly recommended to use version 8. If you are using a later version, you can temporarily downgrade it to 1.8. Otherwise, you will have to install JavaFX separately and set up your environment correctly.

* **Windows and Mac**

As mentioned above, you can install the JDK 8 if you don't still have it. You may have to set the environment variable. Please refer for example to [this site](https://www.java.com/en/download/help/windows_manual_download.html) for more details. Once installed, type `java -version` to check that everything is correct. You should be able to launch the application by simply click on the .jar file or with the command line :

```bash
java -jar osmparser-1.0-SNAPSHOT-launcher.jar
```

* **Ubuntu**

With the linux based OSs, it is a little more tricky to set up javaFX, but there is a script called *linux_setup.sh* that install automatically all required tools. Just type
```bash
sudo bash linux_setup.sh
```
To run the application, type

```bash
sudo bash linux_launcher.sh
```
If you get errors, try to check if you have a valid JDK installed on your machine with `java -version`. If two or more JDKs are installed you can choose which one you want to use with

```bash
sudo update-alternatives --config java
```
and same for javac, the java compiler. (The maven build tool uses the version 11 of java, so you should choose this one). Try to rerun the script and this should be ok.

## Usage

Once Osmosis is installed, you can download a country as .pbf file on geofabrik download server and put it in the /input folder. You also can put your own .osm file but you will have to name it "ways.osm" (**warning : such a file may contain all possible type of ways and result in a very large graph**). On the right pane, you can choose which type of road you want to filter (note that the ..._link categories should be taken with the corresponding type of road for a correct usage). If you want a complete networks, choose all options. (**warning : if your request involves too many nodes, i.e. too large map with too much roads, you will get a java heap out of space memory exception and the program will crash**). You can also pick the bounds you want with left mouse button on the window you can open by clicking on the "choose bounds" button. Make sure that you pick the bounds on the region that corresponds to your .pbf file.
Then click on *import* and wait for osmosis to do its job; (You will see two times `INFOS: Pipeline executing, waiting for completion.` and will have to wait two times until you see `INFOS: Total execution time: .... milliseconds.`). After you have loaded the graph, you can zoom and drag with right mouse button on the map. Click on "show background" if you want to have a more precise idea about where you are. To compute shortest paths, you just have to click twice on the map to choose source and destination nodes.

### **export files**

You can export the graph in CSV format with File -> Export

* raw graph data

All nodes composing ways and all edges will be exported, which can result in a large CSV file

* compressed graph

Only cities nodes and edges between them (computed shortest paths) will be exported. Here is an output exmple :

<p align="center">
  <img src="images/screenshot2.png" style="display: block; margin: 0 auto" />
</p>

To test the precision of those results, we use the matrix distance API of the free service [openrouteservice](https://openrouteservice.org/dev/#/api-docs/v2/matrix/{profile}/post) for the first 50 cities of a data sample around the region of Yverdon-Les-Bains, Switzerland, and compare the results with those given with the OSM-Parser CSV export tool. Since there are 50 cities, we obtain 50 x 50 time routes, one for each pair of cities.

<p align="center">
  <img src="images/screenshot3.png" style="display: block; margin: 0 auto" />
</p>

We can see a correlation. However, the slope of the line that fit the dots is not exactly 1. In fact, we just have to adjust the parameter `SPEED_SMOOTH_FACTOR` in the config file if needed. Below another example with the 50 most important towns of Switzerland.

<p align="center">
  <img src="images/screenshot4.png" style="display: block; margin: 0 auto" />
</p>

Here again, the resuls are quite good, except a group of points far from the regression line. The most extreme point corresponds to the route that link *Visp* to *Spiez*. As you can see in the image below, the shortest path in blue is not the real shortest path. In fact, there is a tunnel between these two places, but it has not been parsed with osmosis in this example.

<p align="center">
  <img src="images/screenshot5.png" style="display: block; margin: 0 auto" />
</p>
