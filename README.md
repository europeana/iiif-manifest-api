**Europeana IIIF API for IIIF v2 manifests**

The Europeana IIIF API is a Java Spring Boot application that on each valid request first retrieves json data from the 
Europeana Record API and uses the response to generate a IIIF v2 manifest.

A  manifest request requires an <a href="https://pro.europeana.eu/get-api">Europeana API key</a> and takes
the following form:
 
`https://<hostname>/presentation/<collectionId>/<recordId>/manifest?wskey=<apikey>`



