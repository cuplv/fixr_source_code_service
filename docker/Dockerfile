FROM ubuntu:16.04

################################################################################
# Install dependencies
################################################################################

RUN apt-get update -qq -y \
    && apt-get install -y openssh-server sudo apt-transport-https software-properties-common libssl-dev time curl wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Java Jdk 8
RUN add-apt-repository -y ppa:webupd8team/java && \
    apt-get update && \
    sudo apt-get install -y openjdk-8-jdk
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

# Sbt
RUN echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
RUN sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
RUN sudo apt-get update
RUN sudo apt-get install -y sbt

################################################################################
# Set up user
# Run everything in the user space now
################################################################################
RUN mkdir /var/run/sshd
RUN chmod 0755 /var/run/sshd
RUN useradd -m srcservice

USER srcservice
WORKDIR /home/srcservice

RUN git clone https://github.com/cuplv/fixr_source_code_service \
    && cd fixr_source_code_service \
    && sbt stage

WORKDIR /home/srcservice/fixr_source_code_service

EXPOSE 8080
ENTRYPOINT ["./target/universal/stage/bin/fixr_source_code_service", "-Dconfig.file=./production_conf/application.conf"]

