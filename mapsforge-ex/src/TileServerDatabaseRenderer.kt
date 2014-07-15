package org.mapsforge.map.layer.renderer

import org.mapsforge.core.graphics.*
import org.mapsforge.core.mapelements.MapElementContainer
import org.mapsforge.core.mapelements.SymbolContainer
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.reader.MapDatabase
import org.mapsforge.map.reader.MapReadResult
import org.mapsforge.map.reader.PointOfInterest
import org.mapsforge.map.rendertheme.RenderCallback
import org.mapsforge.map.rendertheme.rule.RenderTheme

import java.util.*

public class TileServerDatabaseRenderer(private val mapDatabase: MapDatabase?, private val graphicFactory: GraphicFactory) : RenderCallback {
  private val currentLabels = ArrayList<MapElementContainer>()
  private val currentWayLabels = HashSet<MapElementContainer>()
  private var drawingLayers: List<MutableList<ShapePaintContainer>>? = null

  private val ways = arrayOfNulls<ArrayList<MutableList<ShapePaintContainer>>>(LAYERS)

  private val canvas = graphicFactory.createCanvas()

  public var renderTheme: RenderTheme? = null
    set (value) {
      if ($renderTheme == value) {
        return;
      }

      $renderTheme = value
      val levels = value!!.getLevels()
      for (i in 0..LAYERS - 1) {
        var innerWayList = ways[i]
        if (innerWayList == null) {
          innerWayList = ArrayList<MutableList<ShapePaintContainer>>(levels)
          ways[i] = innerWayList
        }
        else {
          innerWayList!!.ensureCapacity(levels)
        }

        for (j in 0..levels - 1) {
          innerWayList!!.add(ArrayList<ShapePaintContainer>(0))
        }
      }
    }

  class object {
    private val LAYERS = 11
    private val TAG_NATURAL_WATER = Tag("natural", "water")

    private fun getTilePixelCoordinates(tileSize: Int): Array<Point> {
      val emptyPoint = Point(0.0, 0.0)
      return array(emptyPoint, Point(tileSize.toDouble(), 0.0), Point(tileSize.toDouble(), tileSize.toDouble()), Point(0.0, tileSize.toDouble()), emptyPoint)
    }

    private fun getValidLayer(layer: Byte): Int {
      if (layer < 0) {
        return 0
      }
      else
        if (layer >= LAYERS) {
          return LAYERS - 1
        }
        else {
          return layer.toInt()
        }
    }

    private fun collisionFreeOrdered(input: List<MapElementContainer>): List<MapElementContainer> {
      // sort items by priority (highest first)
      input.sortBy(Collections.reverseOrder())
      // in order of priority, see if an item can be drawn, i.e. none of the items
      // in the currentItemsToDraw list clashes with it.
      val output = ArrayList<MapElementContainer>(input.size())
      for (item in input) {
        var hasSpace = true
        for (outputElement in output) {
          if (outputElement.clashesWith(item)) {
            hasSpace = false
            break
          }
        }
        if (hasSpace) {
          item.incrementRefCount()
          output.add(item)
        }
      }
      return output
    }
  }

  public fun renderTile(tile: Tile, hasAlpha: Boolean): TileBitmap {
    [suppress("CAST_NEVER_SUCCEEDS")]
    val ways = this.ways as Array<List<MutableList<ShapePaintContainer>>>
    if (mapDatabase != null) {
      processReadMapData(ways, mapDatabase.readMapData(tile), tile)
    }

    val shape = graphicFactory.createTileBitmap(tile.tileSize, hasAlpha) as Shape
    canvas.setBitmap(shape)
    drawWays(ways, shape)

    // now draw the ways and the labels
    drawMapElements(currentWayLabels, tile, shape)
    drawMapElements(collisionFreeOrdered(currentLabels), tile, shape)

    // clear way list
    for (innerWayList in ways) {
      for (shapePaintContainers in innerWayList) {
        shapePaintContainers.clear()
      }
    }

    currentLabels.clear()
    currentWayLabels.clear()

    return shape
  }

  override fun renderArea(way: PolylineContainer, fill: Paint, stroke: Paint?, level: Int) {
    drawingLayers!!.get(level).add(ShapePaintContainer(way, fill, stroke, 0f))
  }

  override fun renderAreaCaption(way: PolylineContainer, priority: Int, caption: String, horizontalOffset: Float, verticalOffset: Float, fill: Paint, stroke: Paint?, position: Position, maxTextWidth: Int) {
    val centerPoint = way.getCenterAbsolute().offset(horizontalOffset.toDouble(), verticalOffset.toDouble())
    currentLabels.add(graphicFactory.createPointTextContainer(centerPoint, priority, caption, fill, stroke, null, position, maxTextWidth))
  }

  override fun renderAreaSymbol(way: PolylineContainer, priority: Int, symbol: Bitmap) {
    currentLabels.add(SymbolContainer(way.getCenterAbsolute(), priority, symbol))
  }

  override fun renderPointOfInterestCaption(poi: PointOfInterest, priority: Int, caption: String, horizontalOffset: Float, verticalOffset: Float, fill: Paint, stroke: Paint?, position: Position, maxTextWidth: Int, tile: Tile) {
    val poiPosition = MercatorProjection.getPixelAbsolute(poi.position, tile.zoomLevel, tile.tileSize)
    currentLabels.add(graphicFactory.createPointTextContainer(poiPosition.offset(horizontalOffset.toDouble(), verticalOffset.toDouble()), priority, caption, fill, stroke, null, position, maxTextWidth))
  }

  override fun renderPointOfInterestCircle(poi: PointOfInterest, radius: Float, fill: Paint, stroke: Paint?, level: Int, tile: Tile) {
    val poiPosition = MercatorProjection.getPixelRelativeToTile(poi.position, tile)
    drawingLayers!!.get(level).add(ShapePaintContainer(CircleContainer(poiPosition, radius), fill, stroke, 0f))
  }

  override fun renderPointOfInterestSymbol(poi: PointOfInterest, priority: Int, symbol: Bitmap, tile: Tile) {
    val poiPosition = MercatorProjection.getPixelAbsolute(poi.position, tile.zoomLevel, tile.tileSize)
    currentLabels.add(SymbolContainer(poiPosition, priority, symbol))
  }

  override fun renderWay(way: PolylineContainer, stroke: Paint, dy: Float, level: Int) {
    drawingLayers!!.get(level).add(ShapePaintContainer(way, null, stroke, dy))
  }

  override fun renderWaySymbol(way: PolylineContainer, priority: Int, symbol: Bitmap, dy: Float, alignCenter: Boolean, repeat: Boolean, repeatGap: Float, repeatStart: Float, rotate: Boolean) {
    WayDecorator.renderSymbol(symbol, priority, dy, alignCenter, repeat, repeatGap, repeatStart, rotate, way.getCoordinatesAbsolute(), currentLabels)
  }

  override fun renderWayText(way: PolylineContainer, priority: Int, textKey: String, dy: Float, fill: Paint, stroke: Paint?) {
    WayDecorator.renderText(textKey, priority, dy, fill, stroke, way.getCoordinatesAbsolute(), currentWayLabels)
  }

  private fun processReadMapData(ways: Array<List<MutableList<ShapePaintContainer>>>, mapReadResult: MapReadResult?, tile: Tile) {
    if (mapReadResult == null) {
      return
    }

    for (pointOfInterest in mapReadResult.pointOfInterests) {
      renderPointOfInterest(ways, pointOfInterest, tile)
    }

    for (way in mapReadResult.ways) {
      renderWay(ways, PolylineContainer(way, tile))
    }

    if (mapReadResult.isWater) {
      renderWaterBackground(ways, tile)
    }
  }

  private fun renderPointOfInterest(ways: Array<List<MutableList<ShapePaintContainer>>>, pointOfInterest: PointOfInterest, tile: Tile) {
    drawingLayers = ways[getValidLayer(pointOfInterest.layer)]
    renderTheme!!.matchNode(this, pointOfInterest, tile)
  }

  private fun renderWaterBackground(ways: Array<List<MutableList<ShapePaintContainer>>>, tile: Tile) {
    drawingLayers = ways[0]
    val coordinates = getTilePixelCoordinates(tile.tileSize)
    renderTheme!!.matchClosedWay(this, PolylineContainer(coordinates, tile, Arrays.asList(TAG_NATURAL_WATER)))
  }

  private fun renderWay(ways: Array<List<MutableList<ShapePaintContainer>>>, way: PolylineContainer) {
    drawingLayers = ways[getValidLayer(way.getLayer())]

    if (way.isClosedWay()) {
      renderTheme!!.matchClosedWay(this, way)
    }
    else {
      renderTheme!!.matchLinearWay(this, way)
    }
  }
}