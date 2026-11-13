.PHONY: generate go-types kotlin-types run-server services-autogenerate ent-schema-generator migrations

SERVICE =
ENV = DEV
SOPS_AGE_KEY_FILE = $(HOME)/.config/sops/age/keys.txt
export SOPS_AGE_KEY_FILE

## Here ENV variable determines to run dev/prod decrpted creds.
SOPS_COMMAND = go tool sops exec-env secrets.$(ENV).env

generate: go-types kotlin-types		## regenerate both sides from services/api.yaml

services-autogenerate: go-types ent-schema-generator

go-types:                      		## regenerate Go types -> services/leptos-agent/gen.go
	go tool oapi-codegen -config services/$(SERVICE)/oapi-codegen.yaml services/leptos-agent/api.yaml

kotlin-types:                  		## regenerate Kotlin models -> shared/payloads/build/generated
	./gradlew :apps:$(SERVICE):shared:openApiGenerate

ent-schema-generator:
	go tool ent generate ./services/$(SERVICE)/schema --target ./services/$(SERVICE)/ent

migrations: 						## migration for ENT schema.
	$(SOPS_COMMAND) 'go run ./services/$(SERVICE)/cmd/migrate'

sops-decrypt:
	go tool sops -d secrets.$(ENV).env

# e.g. make run-server SERVICE=leptos-agent
run-server:							## sops-based migration runs.
	$(SOPS_COMMAND) 'AIR_SERVICE=$(SERVICE) go tool air'
