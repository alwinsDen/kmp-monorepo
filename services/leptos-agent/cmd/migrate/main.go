package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/alwinsden/kmp-monorepo/services/leptos-agent/ent"
	_ "github.com/lib/pq"
)

func main() {
	dbPass := os.Getenv("POSTGRES_DB_PASS")
	dbHost := os.Getenv("POSTGRES_DB_HOST")
	dnPort := os.Getenv("POSTGRES_DB_PORT")
	dbUser := os.Getenv("POSTGRES_DB_USER")

	dsn := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=postgres sslmode=allow", dbHost, dnPort, dbUser, dbPass)
	client, err := ent.Open("postgres", dsn)
	if err != nil {
		panic(err)
	}
	defer client.Close()
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()
	if err := client.Schema.Create(ctx); err != nil {
		log.Fatalf("failed creating schema resources: %v", err)
	}
	println("Schema initialized.")
}
