Mapsforge Tile Server — serve Mapsforge maps. So any application can load tiles from your [map](http://www.openandromaps.org/en/) and your preferred render theme.

Main purpose of this project — ability to use excellent beautiful detailed [Elevate theme](http://www.openandromaps.org/en/legend/elevate-mountain-hike-theme) not only in Android OS.

Current status — alpha.

## Features
* Tiles in the [WebP](https://developers.google.com/speed/webp/) image format if supported by browser and server (currently, only Mac OS X and Linux 64-bit supported as server platform — under Windows/other platforms only PNG (feel free to open issue)).
* Multimap (you can specify folder of maps or several map files).

## Usage
```
Usage: ./bin/mapsforge-tile-server.sh [options]
Options:
 --map (-m) <path1>[,<path2>,...]   : Map files (see http://www.openandromaps.org/en/downloads)
 --theme (-t) <path1>[,<path2>,...] : Render themes (see http://www.openandromaps.org/en/legend), 
                                      file (renderThemes/Elevate/Elevate.xml) or folder (renderThemes)     
 --port (-p)  <int>                 : Port, default 6090 
 --host (-h) VAL                    : Host, default localhost
 --max-file-cache-size (-cs)  <int> : Maximal file cache size in GB, limit is
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
See [nginx configuration example](dist/bin/nginx).