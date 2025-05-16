default:
    @just --list

@repl arg="":
    clojure -M{{arg}}:1.12:dev:test:repl

run *args:
    clojure -M:dev:test:run {{args}}
