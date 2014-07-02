package org.develar.mapsforgeTileServer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;

import static org.develar.mapsforgeTileServer.MapsforgeTileServer.LOG;

public class FileCacheManager {
  private final DB db;
  private final HTreeMap<TileRequest, RenderedTile> fileCache;
  private final BlockingQueue<RemovalNotification<TileRequest, RenderedTile>> flushQueue;

  public FileCacheManager(@NotNull Options options, int executorCount, @NotNull List<Runnable> shutdownHooks) throws IOException {
    File cacheFile = options.cacheFile;
    DBMaker dbMaker = DBMaker.newFileDB(cacheFile).transactionDisable().mmapFileEnablePartial().cacheDisable();
    if (options.maxFileCacheSize != -1) {
      dbMaker.sizeLimit(options.maxFileCacheSize);
    }

    db = createCacheDb(cacheFile, dbMaker);
    fileCache = db.createHashMap("tiles")
      .keySerializer(new TileRequest.TileRequestSerializer())
      .valueSerializer(new RenderedTileSerializer())
      .makeOrGet();

    flushQueue = new ArrayBlockingQueue<>(executorCount * 4);
    Thread flushThread = new Thread(() -> {
      while (true) {
        try {
          RemovalNotification<TileRequest, RenderedTile> removalNotification = flushQueue.take();
          fileCache.put(removalNotification.getKey(), removalNotification.getValue());
        }
        catch (InterruptedException ignored) {
          break;
        }
      }
    }, "Memory to file cache writer");
    flushThread.setPriority(Thread.MIN_PRIORITY);

    shutdownHooks.add(() -> {
      LOG.info("Stop 'Memory to file cache writer' thread");
      flushThread.interrupt();
    });

    flushThread.start();
  }

  public CacheBuilder<TileRequest, RenderedTile> configureMemoryCache(@NotNull CacheBuilder<TileRequest, RenderedTile> cacheBuilder) {
    return cacheBuilder
      .removalListener((RemovalNotification<TileRequest, RenderedTile> notification) -> {
        if (notification.wasEvicted()) {
          flushQueue.add(notification);
        }
      });
  }

  @Nullable
  public RenderedTile get(@NotNull TileRequest tile) {
    return fileCache.get(tile);
  }

  public void close(@NotNull ConcurrentMap<TileRequest, RenderedTile> data) {
    try {
      data.entrySet().parallelStream().forEach(entry -> fileCache.put(entry.getKey(), entry.getValue()));
    }
    finally {
      db.close();
    }
  }

  @NotNull
  private static DB createCacheDb(@NotNull File cacheFile, @NotNull DBMaker dbMaker) throws IOException {
    try {
      return dbMaker.make();
    }
    catch (Throwable e) {
      LOG.error("Cannot open file cache db, db will be recreated", e);
      //noinspection ResultOfMethodCallIgnored
      cacheFile.delete();
      Files.deleteIfExists(Paths.get(cacheFile.getPath(), ".p"));
      return dbMaker.make();
    }
  }
}
