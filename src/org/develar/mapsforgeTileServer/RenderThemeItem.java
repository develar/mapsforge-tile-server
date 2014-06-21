package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.map.rendertheme.rule.RenderTheme;

public final class RenderThemeItem {
  public final RenderTheme awtRenderTheme;
  public final RenderTheme vectorRenderTheme;
  public final String etag;

  public RenderThemeItem(RenderTheme awtRenderTheme, @NotNull RenderTheme vectorRenderTheme, @NotNull String etag) {
    this.awtRenderTheme = awtRenderTheme;
    this.vectorRenderTheme = vectorRenderTheme;
    this.etag = etag;
  }
}
