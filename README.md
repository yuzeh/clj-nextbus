# clj-nextbus

A simple Clojure wrapper around the NextBus API.

## Usage

Dependencies:

```clojure
[org.clojure/data.xml "0.0.7"]
[org.clojure/clojure-contrib "1.2.0"]
[clj-http "0.7.6"]
[camel-snake-kebab "0.1.2"]
```

will publish to clojars soon.

```clojure
(use 'clj-nextbus.core)

(def routes (route-list :agency "MUNI"))

(def my-current-predictions (predictions :stop-id 14985 :agency "MUNI"))
```

## License

Copyright © 2013 Dan Huang

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
