.PHONY: generate go-types kotlin-types run-server

generate: go-types kotlin-types   ## regenerate both sides from services/api.yaml

go-types:                          ## regenerate Go types -> services/leptos-agent/gen.go
	go tool oapi-codegen -config services/leptos-agent/oapi-codegen.yaml services/leptos-agent/api.yaml

kotlin-types:                     ## regenerate Kotlin models -> shared/payloads/build/generated
	./gradlew :apps:leptos-agent:shared:openApiGenerate

SERVICE =

# e.g. make run-server SERVICE=leptos-agent
run-server:
	AIR_SERVICE=$(SERVICE) go tool air
