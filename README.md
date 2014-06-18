Mapsforge Tile Server — serve Mapsforge maps. So any application can load tiles from your [map](http://www.openandromaps.org/en/) and your preferred render theme.

Main purpose of this project — ability to use excellent beautiful detailed [Elevate theme](http://www.openandromaps.org/en/legend/elevate-mountain-hike-theme) not only in Android OS.

Modified mapsforge 'dev' (0.5) version used because in this version label clipping partially fixed.

[Latest release](/releases/latest)

## Features
* Tiles in the [WebP](https://developers.google.com/speed/webp/) image format if supported by browser and server (currently, only Mac OS X and Linux 64-bit supported as server platform — under Windows/other platforms only PNG (feel free to open issue)).
* Multimap (you can specify folder of maps or several map files).

## Public instance and demo 
For personal usage you can use [public mapsforge tile server](http://routeplanner.develar.org) instance instead of setting own.
```javascript
var mapsforgeAttribution = '&copy; <a target="_blank" href="http://www.openandromaps.org/en/disclaimer">OpenAndroMaps</a>';
var mapsforge = L.tileLayer('http://{s}.tile.develar.org/{z}/{x}/{y}', {
  maxZoom: 21,
  attribution: 'tiles ' + mapsforgeAttribution
});
```
Coverage is limited currently, feel free [to open issue](https://github.com/develar/mapsforge-tile-server/issues/new). Covered countries:
* [Alps](http://www.openandromaps.org/wp-content/images/maps/europe/Alps.jpg)
* [Germany](http://www.openandromaps.org/wp-content/images/maps/europe/Germany.jpg)
* [France](http://www.openandromaps.org/wp-content/images/maps/europe/France.jpg)
* [Great Britain](http://www.openandromaps.org/wp-content/images/maps/europe/Great_Britain.jpg)
* [Italy](http://www.openandromaps.org/wp-content/images/maps/europe/Italy.jpg)
* [Spain, Portugal](http://www.openandromaps.org/wp-content/images/maps/europe/Spain_Portugal.jpg)
* [Poland](http://www.openandromaps.org/wp-content/images/maps/europe/Poland.jpg)
* [Finland](http://www.openandromaps.org/wp-content/images/maps/europe/Finland.jpg)
* [Belarus](http://www.openandromaps.org/wp-content/images/maps/europe/Belarus.jpg)
* [Balkan](http://www.openandromaps.org/wp-content/images/maps/europe/Balkan.jpg)

Usage policy the same as [OSM](http://wiki.openstreetmap.org/wiki/Tile_usage_policy).

## Usage
```
Usage: ./bin/mapsforge-tile-server.sh [options]
Options:
 --map (-m) <path1>[,<path2>,...]     : Map files (see http://www.openandromaps.org/en/downloads)
 --theme (-t) <path1>[,<path2>,...]   : Render themes (see http://www.openandromaps.org/en/legend), 
                                        file (renderThemes/Elevate/Elevate.xml) or folder (renderThemes)     
 --port (-p)  <int>                   : Port, default 6090 
 --host (-h) VAL                      : Host, default localhost
 --max-file-cache-size (-cs) <double> : Maximal file cache size in GB, limit is
                                        not strict, actual size might be 10% or
                                        more bigger. Set -1 to unlmited
```

Only `--map` option is required. [Elevate theme](http://www.openandromaps.org/en/legend/elevate-mountain-hike-theme) will be used by default.

### Several maps
```
./bin/mapsforge-tile-server.sh --map ~/maps/Alps.map,~/maps/Germany.map
```
or
```
./bin/mapsforge-tile-server.sh --map /path/to/maps
```

### Nginx
See [nginx configuration example](dist/conf/nginx).