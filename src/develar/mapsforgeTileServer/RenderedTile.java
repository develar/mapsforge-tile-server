package develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class RenderedTile {
  private final byte[] data;
  public long lastModified = Instant.now().getEpochSecond();

  public RenderedTile(@NotNull byte[] data) {
    this.data = data;
  }

  @NotNull
  public byte[] getData() {
    return data;
  }
}
