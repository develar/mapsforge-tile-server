Mapsforge Tile Server — serve Mapsforge maps. So any application can load tiles from your [map](http://www.openandromaps.org/en/) and your preferred render theme.

Main purpose of this project — ability to use excellent beautiful detailed [Elevate theme](http://www.openandromaps.org/en/legend/elevate-mountain-hike-theme) not only in Android OS.

Current status — alpha.

## Features
* Tiles in the [WebP](https://developers.google.com/speed/webp/) image format (if supported by browser).

## Command line usage
```
	--map: [required] Map files (see http://www.openandromaps.org/en/downloads)
	--theme: [required] Render theme (see http://www.openandromaps.org/en/legend) file (renderThemes/Elevate/Elevate.xml) or folder (renderThemes)
  --memory-cache-spec: [optional] Memory cache spec, see http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/CacheBuilder.html
```