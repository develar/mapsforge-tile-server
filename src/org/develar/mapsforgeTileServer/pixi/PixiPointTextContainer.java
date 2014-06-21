package org.develar.mapsforgeTileServer.pixi;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;

public class PixiPointTextContainer extends PointTextContainer {
  /**
   * Create a new point container, that holds the x-y coordinates of a point, a text variable, two paint objects, and
   * a reference on a symbolContainer, if the text is connected with a POI.
   *
   * @param point
   * @param priority
   * @param text
   * @param paintFront
   * @param paintBack
   * @param symbolContainer
   * @param position
   * @param maxTextWidth
   */
  protected PixiPointTextContainer(Point point,
                                   int priority,
                                   String text,
                                   Paint paintFront,
                                   Paint paintBack,
                                   SymbolContainer symbolContainer, Position position, int maxTextWidth) {
    super(point, priority, text, paintFront, paintBack, symbolContainer, position, maxTextWidth);

    boundary = computeBoundary();
  }

  @Override
  public void draw(Canvas canvas, Point origin, Matrix matrix) {
  }

  private Rectangle computeBoundary() {
    int lines = textWidth / maxTextWidth + 1;
    double boxWidth = textWidth;
    double boxHeight = textHeight;

    if (lines > 1) {
      // a crude approximation of the size of the text box
      boxWidth = maxTextWidth;
      boxHeight = textHeight * lines;
    }

    switch (position) {
      case CENTER:
        return new Rectangle(-boxWidth / 2, -boxHeight / 2, boxWidth / 2, boxHeight / 2);
      case BELOW:
        return new Rectangle(-boxWidth / 2, 0, boxWidth / 2, boxHeight);
      case ABOVE:
        return new Rectangle(-boxWidth / 2, -boxHeight, boxWidth / 2, 0);
      case LEFT:
        return new Rectangle(-boxWidth, -boxHeight / 2, 0, boxHeight / 2);
      case RIGHT:
        return new Rectangle(0, -boxHeight / 2, boxWidth, boxHeight / 2);
    }
    return null;
  }
}
