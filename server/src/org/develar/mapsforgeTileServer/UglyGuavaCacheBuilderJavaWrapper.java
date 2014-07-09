package org.develar.mapsforgeTileServer;

import com.google.common.cache.CacheBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforge.map.rendertheme.rule.MatchingCacheKey;

import java.util.List;
import java.util.Map;

public final class UglyGuavaCacheBuilderJavaWrapper {
  private static final CacheBuilder<Object, Object> CACHE_BUILDER = CacheBuilder.newBuilder().maximumSize(1024);

  public static Map<MatchingCacheKey, List<RenderInstruction>> createCache() {
    return CACHE_BUILDER.<MatchingCacheKey, List<RenderInstruction>>build().asMap();
  }
}
