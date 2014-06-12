package org.develar.mapsforgeTileServer;

import org.kohsuke.args4j.Option;

import java.io.File;
import java.nio.file.Path;

public class Options {
  @Option(name = "--map", aliases = {"-m"}, usage = "Map files (see http://www.openandromaps.org/en/downloads)", required = true)
  public Path[] maps;

  @Option(name = "--theme", aliases = {"-t"}, usage = "Render theme (see http://www.openandromaps.org/en/legend) file (renderThemes/Elevate/Elevate.xml)" +
    " or folder (renderThemes)", required = true)
  public Path theme;

  @Option(name = "--port", aliases = {"-p"})
  public int port = 6090;

  @Option(name = "--host", aliases = {"-h"})
  public String host;

  @Option(name = "--max-file-cache-size", aliases = {"-cs"}, usage = "Maximal file cache size in GB, limit is not strict, actual size might be 10% or more bigger. Set -1 to unlmited")
  public double maxFileCacheSize = 30;

  @Option(name = "--cache-file", aliases = {"-c"}, usage = "File cache")
  public File cacheFile = new File("mapsforge-tiles.cache");
}
