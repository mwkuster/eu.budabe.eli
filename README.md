# eu.budabe.eli

A Clojure library designed to build prototypical European Legislation Identifiers (ELIs) based on Cellar data

## Usage

lein compile

lein ring server

## Precondition

Fuseki as server backend
===========================
Download latest version of Fuseki and unzip. Run the server with
```
./fuseki-server -loc=/var/lib/fuseki/eli -update /eli
```

Storage location chosen to be /var/lib/fuseki/eli (any other location will work just as well)

```
curl -v -XPUT -T 32012L0012.ttl --header "Content-type: text/turtle;charset=utf-8" http://localhost:3030/eli/data?graph=http%3A%2F%2Fpublications.europa.eu%2Fresource%2Fcelex%2F32012L0012

curl -v -XPUT -T 32012L0012R%2801%29.ttl --header "Content-type: text/turtle;charset=utf-8" http://localhost:3030/eli/data?graph=http%3A%2F%2Fpublications.europa.eu%2Fresource%2Fcelex%2F32012L0012R%2801%29

curl -v http://localhost:3030/eli/data?graph=http%3A%2F%2Fpublications.europa.eu%2Fresource%2Fcelex%2F32012L0012
```

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

