# https://hub.docker.com/_/openjdk/
FROM ubuntu

# Copy file to ramble dir.
COPY . /usr/src/ramble

# Set the working dir to ramble.
WORKDIR /usr/src/ramble

# Install bash.
RUN apt-get update
RUN apt-get -y install openjdk-8-jdk
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
