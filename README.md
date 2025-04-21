A bridge to transform a Mastodon list into an RSS feed.

## Docker instructions

### Building the image

```
docker image rm bodlulu/mastodon-to-rss:latest
docker build --platform linux/x86_64 -t bodlulu/mastodon-to-rss .
```

### Running the image

```
docker pull bodlulu/mastodon-to-rss
docker run -p <PORT TO LISTEN TO>:8080 bodlulu/mastodon-to-rss
```
