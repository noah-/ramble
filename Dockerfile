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
RUN ./gradlew clean assemble
RUN tar -xvf ramble-distribution*.gz
RUN apt-get -y install inotify-tools
RUN apt-get -y install iproute2
RUN apt-get -y install iputils-ping
RUN apt-get -y install net-tools


# Make port 80 available to the world outside this container
EXPOSE 80

# Define environment variable
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

# Run app.py when the container launches
# TODO(JK), add a wrapper for the cli.
# CMD ["./ramble-distribution/bin/ramble-cli.sh"]
CMD ["sleep", "6000"]
