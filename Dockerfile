# https://hub.docker.com/_/openjdk/
FROM  openjdk:alpine

# Copy file to ramble dir.
COPY . /usr/src/ramble

# Set the working dir to ramble.
WORKDIR /usr/src/ramble

# Install bash.
RUN apk update
RUN apk add bash bash-doc bash-completion
# Build ramble.
RUN ./gradlew clean build
RUN tar -xvf ramble-distribution*.gz

# Make port 80 available to the world outside this container
EXPOSE 80

# Define environment variable
ENV NAME World

# Run app.py when the container launches
# TODO(JK), add a wrapper for the cli.
# CMD ["./ramble-distribution/bin/ramble-cli.sh"]
CMD ["sleep", "6000"]
