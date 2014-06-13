package org.develar.mapsforgeTileServer;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MultiPathOptionHandlerEx extends OptionHandler<Path> {
  private static final Splitter SPLITTER = Splitter.on(CharMatcher.anyOf(",;:")).omitEmptyStrings().trimResults();

  public MultiPathOptionHandlerEx(CmdLineParser parser, OptionDef option, Setter<? super Path> setter) {
    super(parser, option, setter);
  }

  @Override
  public int parseArguments(Parameters params) throws CmdLineException {
    for (String value : SPLITTER.split(params.getParameter(0))) {
      if (value.startsWith("~/") || value.startsWith("~\\")) {
        value = System.getProperty("user.home") + value.substring(1);
      }

      Path path;
      try {
        path = Paths.get(value);
      }
      catch (Exception e) {
        throw new CmdLineException(owner, "Failed to Parse Path: " + value, e);
      }

      setter.addValue(path);
    }

    return 1;
  }
  @Override
  public String getDefaultMetaVariable() {
    return "<path1>[,<path2>,...]";
  }
}
