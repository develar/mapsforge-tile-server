package org.develar.mapsforgeTileServer;

import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.IntMap;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

// java.lang.IllegalAccessError: tried to access class com.badlogic.gdx.utils.IntMap$MapIterator from class org.develar.mapsforgeTileServer.RenderThemeManager
public class UglyKotlin {
  //val sortedCharCodes = IntArray(chars.size)
  //var j = 0
  //while (charCodes.hasNext) {
  //  sortedCharCodes[j++] = charCodes.next()
  //}
  //sortedCharCodes.sort()
  @NotNull
  public static <T> int[] getSortedKeys(@NotNull IntMap<T> intMap) {
    IntMap.Keys keys = intMap.keys();
    int[] result = new int[intMap.size];
    int i = 0;
    while (keys.hasNext) {
      result[i++] = keys.next();
    }
    Arrays.sort(result);
    return result;
  }

  @NotNull
  public static int[] getSortedKeys(@NotNull IntIntMap intMap) {
    IntIntMap.Keys keys = intMap.keys();
    int[] result = new int[intMap.size];
    int i = 0;
    while (keys.hasNext) {
      result[i++] = keys.next();
    }
    Arrays.sort(result);
    return result;
  }
}