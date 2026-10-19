package main

import (
	"context"
	"log"
	"net/http"

	"github.com/alwinsden/kmp-monorepo/services/leptos-agent/openrouter"
	"github.com/joho/godotenv"
)

type Server struct{}

func (s *Server) GetHealth(ctx context.Context, request GetHealthRequestObject) (GetHealthResponseObject, error) {
	return GetHealth200JSONResponse{Status: "Health is good."}, nil
}

func (s *Server) SubmitConversation(ctx context.Context, request SubmitConversationRequestObject) (SubmitConversationResponseObject, error) {
	response, err := openrouter.GetMachineResponse(request.Body.Prompt)
	if err != nil {
		return nil, nil
	}
	return SubmitConversation200JSONResponse{ConversationId: "sample_id", Message: *response}, nil
}

func main() {
	mux := http.NewServeMux()
	err := godotenv.Load()
	if err != nil {
		log.Fatal("Error loading .env file.")
	}

	/*
		HandlerFromMux is from @api.yaml auto-generated code,
		which registers the handlers for the API endpoints defined
		in the OpenAPI specification.
	*/
	HandlerFromMux(NewStrictHandler(&Server{}, nil), mux)

	addr := ":8080"
	log.Printf("leptos-agent service listening on %s", addr)
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatal(err)
	}
}
