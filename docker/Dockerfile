FROM ubuntu:16.04

################################################################################
# Install dependencies
################################################################################

RUN apt-get update -qq -y \
    && apt-get install -y openssh-server sudo apt-transport-https software-properties-common libssl-dev time curl wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Oracle Java Jdk 8
RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk7-installer
# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

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

RUN mkdir .ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts

RUN echo "Clone and build repository..." \
    && git clone https://github.com/cuplv/fixr_source_code_service \
    && cd fixr_source_code_service \
    && sbt stage

WORKDIR /home/srcservice/fixr_source_code_service

EXPOSE 8080
ENTRYPOINT ["./target/universal/stage/bin/fixr_source_code_service", "-Dconfig.file=./production_conf/application.conf"]

