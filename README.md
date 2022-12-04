A bridge to transform a Mastodon list into an RSS feed.

## Docker instructions

### Building and pushing the image to Docker Hub

```
docker image rm bodlulu/mastodon-to-rss:latest
DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage
```

### Running the image

```
docker pull bodlulu/mastodon-to-rss
docker run -p <PORT TO LISTEN TO>:8080 bodlulu/mastodon-to-rss
```
