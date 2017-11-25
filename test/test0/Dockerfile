# https://hub.docker.com/_/openjdk/
FROM  alpine

# Copy file to ramble dir.
COPY . /usr/src/test

# Set the working dir to ramble.
WORKDIR /usr/src/test

# Install Dependencies.
RUN apk update
RUN apk add inotify-tools
RUN apk add bash

# Make port 80 available to the world outside this container
EXPOSE 80

# Define environment variable
# ENV NAME World

# Run app.py when the container launches
# TODO(JK), add a wrapper for the cli.
# CMD ["./ramble-distribution/bin/ramble-cli.sh"]
CMD ["/usr/src/test/on_start.sh"]
