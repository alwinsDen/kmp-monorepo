.PHONY: generate go-types kotlin-types run-server

SERVICE =

generate: go-types kotlin-types    ## regenerate both sides from services/api.yaml

services-autogenerate: go-types ent-schema-generator

go-types:                          ## regenerate Go types -> services/leptos-agent/gen.go
	go tool oapi-codegen -config services/$(SERVICE)/oapi-codegen.yaml services/leptos-agent/api.yaml

kotlin-types:                     ## regenerate Kotlin models -> shared/payloads/build/generated
	./gradlew :apps:$(SERVICE):shared:openApiGenerate

ent-schema-generator:
	go tool ent generate ./services/$(SERVICE)/schema --target ./services/$(SERVICE)/ent

# e.g. make run-server SERVICE=leptos-agent
run-server:
	AIR_SERVICE=$(SERVICE) go tool air
