package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

import java.awt.image.BufferedImage;

public interface TileRenderer {
  @NotNull
  BufferedImage render(@NotNull Tile tile);
}
