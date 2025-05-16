# scram

it's like [cram](https://bitheap.org/cram/) or [ppx_expect](https://github.com/janestreet/ppx_expect) but for clojure.

## what's it do

define some tests where you want to compare the `*out*` of a given call to a string literal. run the provided cli over the namespaces, and if the output doesn't match the given string, it'll print a nifty little git-like diff. if you run the cli with `--write`, it'll update your files in place.

### example

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

pretty simple.

## api

* `noahtheduke.scram/defcram`: create a function var with two arities.

## usage with other tools

if you wanna have the fancy comparison in a clojure.test deftest or in a lazytest test, there are extension namespaces that provide help.

to support `clojure.test`, `noahtheduke.scram.extensions.clojure-test` defines `defcram`, which is the same as `noahtheduke.scram/defcram` but adds `:test` metadata to make them run during `clojure.test` runs (through leiningen, kaocha, cognitect.test-runner, etc). it also defines an `assert-expr` for `compare-output` so failures are pretty-printed.

to support `lazytest`, `noahtheduke.scram.extensions.lazytest` defines `defcram` which is the same as `noahtheduke.scram/defcram` but adds `:lazytest/test` metadata to make them run during `lazytest` runs. it also defines an alternative `compare-output` which throws an `lazytest.ExceptionFailed`.

## License

Copyright © Noah Bogart

Distributed under the Mozilla Public License version 2.0.
