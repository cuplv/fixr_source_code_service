# Web service component used to query source code from github repositories

The web service has the following end-points:

## get_method_code

The input of the services are:
- github_repo_url: the url to the github repository
- commit_hash: the hash of the commits
- class_name: the name of the Java class containing the method (with the package name)
- method_signature: the signature of the method to retreive
