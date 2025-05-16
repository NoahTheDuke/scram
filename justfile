default:
    @just --list

@repl arg="":
    clojure -M:provided:1.12:dev:test{{arg}}:repl

run *args:
    clojure -M:dev:test:run {{args}}
