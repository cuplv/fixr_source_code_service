# Web service component used to query source code from github repositories

The web service has the following end-points:

## get_method_code

The input of the services are:
- github_repo_url: the url to the github repository
- commit_hash: the hash of the commits
- class_name: the name of the Java class containing the method (with the package name)
- method_signature: the signature of the method to retreive

# Deployment

Create the deployment package:

```
$> sbt stage
```

Run the binary with the production configuration:
```
$> ./target/universal/stage/bin/fixr_source_code_service -Dconfig.file="/production_conf/application.conf"
```

# Docker container

```
$> cd docker
$> docker image build -t fixr_source_code_service  .
$> docker run -p 8080:8080 -d fixr_source_code_service
```
