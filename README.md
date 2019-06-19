**Europeana IIIF Manifest API for IIIF v2 and v3 manifests**

The Europeana IIIF Manifest API is a Java Spring-Boot 2 application that on each valid request first retrieves json data from the 
Europeana Record API and uses the response to generate either an IIIF v2 or v3 manifest.

The Manifest API will also check with the Europeana Full-Text API if the record has fulltext available. If that is the case
then a link to the appropriate IIIF Fulltext API url is added in the manifest.

A  manifest request requires an <a href="https://pro.europeana.eu/get-api">Europeana API key</a> and takes
the following form:
 
`https://<hostname>/presentation/<collectionId>/<recordId>/manifest?wskey=<apikey>`

By default a IIIF v2 will be generated, but you can specify which version you want by adding 
 
 - a format parameter, e.g. `https://<hostname>/presentation/<collectionId>/<recordId>/manifest?wskey=<apikey>&format=3`
 - or add an accept header to your request, e.g. `Accept: application/json; profile="http://iiif.io/api/presentation/3/context.json"`



