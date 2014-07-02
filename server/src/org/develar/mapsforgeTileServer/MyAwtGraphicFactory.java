package org.develar.mapsforgeTileServer;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.awt.AwtPaint;
import org.mapsforge.map.awt.AwtTileBitmap;

import java.awt.image.BufferedImage;

class MyAwtGraphicFactory extends AwtGraphicFactory {
  //private static final Font[] ROBOTO;

  //static {
  //  Font[] roboto;
  //  try {
  //    roboto = new Font[]{
  //      //Font.createFont(Font.TRUETYPE_FONT, MyAwtGraphicFactory.class.getResourceAsStream("/Roboto-Regular.ttf")),
  //      //Font.createFont(Font.TRUETYPE_FONT, MyAwtGraphicFactory.class.getResourceAsStream("/Roboto-Bold.ttf")),
  //      //Font.createFont(Font.TRUETYPE_FONT, MyAwtGraphicFactory.class.getResourceAsStream("/Roboto-Italic.ttf")),
  //      //Font.createFont(Font.TRUETYPE_FONT, MyAwtGraphicFactory.class.getResourceAsStream("/Roboto-BoldItalic.ttf"))
  //      //Font.createFont(Font.TRUETYPE_FONT, MyAwtGraphicFactory.class.getResourceAsStream("/OpenSans-Regular.ttf")),
  //      //Font.createFont(Font.TRUETYPE_FONT, MyAwtGraphicFactory.class.getResourceAsStream("/bold.ttf")),
  //      //Font.createFont(Font.TRUETYPE_FONT, MyAwtGraphicFactory.class.getResourceAsStream("/OpenSans-Italic.ttf")),
  //      //Font.createFont(Font.TRUETYPE_FONT, MyAwtGraphicFactory.class.getResourceAsStream("/OpenSans-SemiboldItalic.ttf"))
  //    };
  //  }
  //  catch (FontFormatException | IOException e) {
  //    MapsforgeTileServer.LOG.error(e.getMessage(), e);
  //    roboto = new Font[]{};
  //  }
  //
  //  ROBOTO = roboto;
  //}

  @Override
  public org.mapsforge.core.graphics.Paint createPaint() {
    return new AwtPaint() {
      @Override
      protected void createFont() {
        //if (!Font.SERIF.equals(fontName)) {
          super.createFont();
        //}
        //
        //font = textSize > 0 ? ROBOTO[fontStyle].deriveFont(textSize) : null;
      }
    };
  }

  @Override
  public TileBitmap createTileBitmap(int tileSize, boolean hasAlpha) {
    return new AwtTileBitmap(new BufferedImage(tileSize, tileSize, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_3BYTE_BGR));
  }
}
