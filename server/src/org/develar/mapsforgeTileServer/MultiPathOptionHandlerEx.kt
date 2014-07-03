package org.develar.mapsforgeTileServer

import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.OptionDef
import org.kohsuke.args4j.spi.OptionHandler
import org.kohsuke.args4j.spi.Parameters
import org.kohsuke.args4j.spi.Setter

import java.nio.file.Path
import java.nio.file.Paths

private val SPLITTER = Splitter.on(CharMatcher.anyOf(",;:")).omitEmptyStrings().trimResults()

public class MultiPathOptionHandlerEx(parser: CmdLineParser, option: OptionDef, setter: Setter<in Path>) : OptionHandler<Path>(parser, option, setter) {
  throws(javaClass<CmdLineException>())
  override fun parseArguments(params: Parameters): Int {
    for (value in SPLITTER.split(params.getParameter(0))) {
      val normalizedValue:String;
      if (value.startsWith("~/") || value.startsWith("~\\")) {
        normalizedValue = System.getProperty("user.home") + value.substring(1)
      }
      else {
        normalizedValue = value;
      }

      try {
        setter.addValue(Paths.get(normalizedValue))
      }
      catch (e: Exception) {
        throw CmdLineException(owner, "Failed to Parse Path: " + value, e)
      }
    }

    return 1
  }

  override fun getDefaultMetaVariable(): String {
    return "<path1>[,<path2>,...]"
  }
}
