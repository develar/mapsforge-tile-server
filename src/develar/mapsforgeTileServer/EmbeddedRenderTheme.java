package develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;

import java.io.InputStream;

public enum EmbeddedRenderTheme implements XmlRenderTheme {
  ELEVATE("/Elevate/", "Elevate.xml");

  private final String absolutePath;
  private final String file;

  private EmbeddedRenderTheme(@NotNull String absolutePath, @NotNull String file) {
    this.absolutePath = absolutePath;
    this.file = file;
  }

  @Override
  public XmlRenderThemeMenuCallback getMenuCallback() {
    return null;
  }

  @Override
  public String getRelativePathPrefix() {
    return absolutePath;
  }

  @Override
  public InputStream getRenderThemeAsStream() {
    return EmbeddedRenderTheme.class.getResourceAsStream(absolutePath + file);
  }
}
