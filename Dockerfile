FROM ubuntu:20.04
LABEL maintainer="BoD <BoD@JRAF.org>"

# Copy the binary
COPY build/bin/linuxX64/releaseExecutable/mastodon-to-rss.kexe mastodon-to-rss.kexe

# Install libcurl
RUN apt-get update && apt-get install -y libcurl4-openssl-dev

EXPOSE 8080

ENTRYPOINT ["./mastodon-to-rss.kexe"]
