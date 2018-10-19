# Azure App Service Plugin Changelog

## Version 0.4.1, 2018-10-19
* Fix security vulnerability of jackson-databind

## Version 0.4.0, 2018-09-10
* Support zip deploy for Java SE applications
* Add retry logic for war deploy
* Fix exception for failing to find Azure resources

## Version 0.3.1, 2018-05-30
* Switch to war deploy for Java apps
* Add null check for path names when deploy

## Version 0.3.0, 2018-04-03
* Add an option to skip docker build step (#25)
* Restart slot after successful deployment (JENKINS-48191, #21)
* Support Java container app on Linux (#23)
* Support for credentials lookup in [Folders](https://plugins.jenkins.io/cloudbees-folder)
* Expands variables in source/target directory and slot name (#24)

## Version 0.2.0, 2018-01-05
* Support MSI

## Version 0.1.3, 2017-11-07
* Specify refspec explicitly when doing git push
* Add Third Party Notice

## Version 0.1.2, 2017-10-18
* Remove runtime licenses

## Version 0.1.1, 2017-09-30
* Improve stability of FTP and Docker deploy

## Version 0.1, 2017-07-18
* Initial release
* Support deploy to Azure Web App through Git and FTP
* Support deploy to Azure Web App on Linux through Docker
