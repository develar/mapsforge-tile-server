package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.map.rendertheme.rule.RenderTheme;

public final class RenderThemeItem {
  public final RenderTheme renderTheme;
  public final String etag;

  public RenderThemeItem(@NotNull RenderTheme renderTheme, @NotNull String etag) {
    this.renderTheme = renderTheme;
    this.etag = etag;
  }
}
