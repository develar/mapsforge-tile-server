package org.develar.mapsforgeTileServer

import org.kohsuke.args4j.Option
import java.io.File
import java.nio.file.Path

class Options() {
  Option(name = "--map", aliases = arrayOf("-m"), usage = "Map files (see http://www.openandromaps.org/en/downloads)", required = true, handler = MultiPathOptionHandlerEx::class)
  public var maps: Array<Path>? = null

  Option(name = "--theme", aliases = arrayOf("-t"), usage = "Render themes (see http://www.openandromaps.org/en/legend), " + "file (renderThemes/Elevate/Elevate.xml)" + " or folder (renderThemes)", required = true, handler = MultiPathOptionHandlerEx::class)
  public var themes: Array<Path>? = null

  Option(name = "--host", aliases = arrayOf("-h"), usage = "Host, localhost by default")
  public var host: String? = null

  Option(name = "--port", aliases = arrayOf("-p"), usage = "Port")
  public var port: Int = 80

  Option(name = "--max-file-cache-size", aliases = arrayOf("-cs"), usage = "Maximal file cache size in GB, limit is not strict, actual size might be 10% or more bigger. Set -1 to unlmited")
  public var maxFileCacheSize: Double = -2.0

  Option(name = "--cache-file", aliases = arrayOf("-c"), usage = "File cache")
  public var cacheFile: File = File("mapsforge-tiles.cache")
}