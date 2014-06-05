package develar.mapsforgeTileServer;

import org.kohsuke.args4j.Option;

import java.io.File;

public class Options {
  @Option(name = "--map", aliases = {"-m"}, usage = "Map files (see http://www.openandromaps.org/en/downloads)", required = true)
  public File[] maps;

  @Option(name = "--theme", aliases = {"-t"}, usage = "Render theme (see http://www.openandromaps.org/en/legend) file (renderThemes/Elevate/Elevate.xml)" +
    " or folder (renderThemes)", required = true)
  public String theme;

  @Option(name = "--port", aliases = {"-p"})
  public int port = 17778;

  @Option(name = "--host", aliases = {"-h"})
  public String host;

  @Option(name = "--memory-cache-spec", aliases = {"-mcs"}, usage = "Memory cache spec, see http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/CacheBuilder.html")
  public String memoryCacheSpec = "maximumSize=" + (1024 * 10);

  @Option(name = "--cache-file", aliases = {"-cf"}, usage = "SQLite file cache")
  public File cacheFile;
}
