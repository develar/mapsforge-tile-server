mapsforge-tile-server is a high performance server (especially on Linux 64-bit — native epoll supported) and you don't need to use [Nginx](http://nginx.org) in front of just to reduce connections.

mapsforge-tile-server supported not only in-memory cache, but file cache too, so, really, no difference.

But if you need to use standard HTTP port instead of custom (by default, 6090), you can setup nginx to proxy requests. In this case recommended to disable mapsforge-tile-server file cache (--max-file-cache-size 0) and configure [nginx content caching](http://nginx.com/resources/admin-guide/caching/).