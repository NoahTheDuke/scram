# scram

it's like [cram](https://bitheap.org/cram/) or [ppx_expect](https://github.com/janestreet/ppx_expect) but for clojure.

## what's it do

define some tests where you want to compare the `*out*` of a given call to a string literal. run the provided cli over the namespaces, and if the output doesn't match the given string, it'll print a nifty little git-like diff. if you run the cli with `--write`, it'll update your files in place.

comes with support for `clojure.test` (and `lazytest` soon).

given:

```clojure
(ns cool.namespace)

(def example
  "
(ns foo)
(+ 1 2 3
   4 5 6)
")

(defcram hello
  (compare-output
    (print example)
    "hello\n"))
```

running `clojure -M:scram cool.namespace` will print

```diff
--- /my/cool/namespace.clj
+++ /my/cool/namespace.clj
@@ -15,3 +15,7 @@
   (compare-output
    (print example)
-   "hello\n"))
+   "
+(ns foo)
+(+ 1 2 3
+   4 5 6)
+"))
```

and running `clojure -M:scram --write cool.namespace` will print the same and then apply that change to the file itself.

pretty simple, pretty friggin cool imo

## License

Copyright © Noah Bogart

Distributed under the Mozilla Public License version 2.0.
