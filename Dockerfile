FROM jeanblanchard/busybox-java:8
MAINTAINER Vladimir Krivosheev <develar@gmail.com>

WORKDIR app

COPY out/artifacts/mapsforge-tile-server.jar mapsforge-tile-server.jar
COPY out/atlases atlases
COPY dist/renderThemes renderThemes

COPY lib/*.so lib/*.dylib lib/*.so lib/*.dll lib/marlin-* lib/

EXPOSE 80
ENTRYPOINT ["java", "-server", "-Xmx768m", "-Dfile.encoding=UTF-8", "-Djava.awt.headless=true", \
  "-Xbootclasspath/a:lib/marlin-0.5.5-Unsafe.jar", \
  "-Dsun.java2d.renderer=org.marlin.pisces.PiscesRenderingEngine", "-Dsun.java2d.renderer.useRef=hard", \
  "-Dorg.slf4j.simpleLogger.showLogName=false", \
  "-Dio.netty.machineId=9e43d860", \
  "-Djava.security.egd=file:/dev/urandom", \
  "-Djava.library.path=lib", "-jar", "mapsforge-tile-server.jar", \
  "-h", "0.0.0.0", "--theme", "renderThemes", "--map", "/maps", "--max-file-cache-size=8"]