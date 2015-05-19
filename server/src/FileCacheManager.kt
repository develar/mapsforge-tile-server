package org.develar.mapsforgeTileServer

import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.HTreeMap

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentMap

class FileCacheManager(options: Options, executorCount: Int, shutdownHooks: MutableList<() -> Unit>) {
  private val db: DB
  private val fileCache: HTreeMap<TileRequest, RenderedTile>
  private val flushQueue: BlockingQueue<RemovalNotification<in TileRequest, in RenderedTile>>

  init {
    val cacheFile = options.cacheFile
    val dbMaker = DBMaker.newFileDB(cacheFile).transactionDisable()!!.mmapFileEnablePartial().cacheDisable()
    if (options.maxFileCacheSize != -1.0) {
      dbMaker.sizeLimit(options.maxFileCacheSize)
    }

    try {
      db = dbMaker.make()
    }
    catch (e: Throwable) {
      LOG.error("Cannot open file cache db, db will be recreated", e)
      cacheFile.delete()
      Files.deleteIfExists(Paths.get(cacheFile.getPath(), ".p"))
      db = dbMaker.make()
    }

    fileCache = db.createHashMap("tiles").keySerializer(TileRequest.TileRequestSerializer()).valueSerializer(RenderedTileSerializer()).makeOrGet()

    flushQueue = ArrayBlockingQueue<RemovalNotification<in TileRequest, in RenderedTile>>(executorCount * 4)
    val flushThread = Thread("Memory to file cache writer")
    flushThread.setPriority(Thread.MIN_PRIORITY)

    shutdownHooks.add {
      LOG.info("Stop 'Memory to file cache writer' thread");
      flushThread.interrupt();
    }

    flushThread.start()
  }

  public fun configureMemoryCache(cacheBuilder: CacheBuilder<TileRequest, RenderedTile>): CacheBuilder<TileRequest, RenderedTile> {
    return cacheBuilder.removalListener(fun (removalNotification: RemovalNotification<in TileRequest, in RenderedTile>): Unit = if (removalNotification.wasEvicted()) {
      flushQueue.add(removalNotification)
    })
  }

  public fun get(tile: TileRequest): RenderedTile? {
    return fileCache.get(tile)
  }

  public fun close(data: ConcurrentMap<TileRequest, RenderedTile>) {
    try {
      for ((request, tile) in data) {
        fileCache.put(request, tile)
      }
    }
    finally {
      db.close()
    }
  }
}