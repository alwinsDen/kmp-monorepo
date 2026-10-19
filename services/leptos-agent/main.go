package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	commonstructs "github.com/alwinsden/kmp-monorepo/services/commonStructs"
	"github.com/alwinsden/kmp-monorepo/services/leptos-agent/openrouter"
	"github.com/joho/godotenv"
)

type Server struct {
	strict ServerInterface
}

func (s *Server) GetHealth(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(GetHealth200JSONResponse{Status: "Up and running."})
}

func (s *Server) SubmitConversation(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Expose-Headers", "Content-Type")

	var body SubmitConversationJSONRequestBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(BadRequest{Error: err.Error()})
	}

	w.Header().Set("Content-Type", "text/event-stream")
	w.Header().Set("Cache-Control", "no-cache")
	w.Header().Set("Connection", "keep-alive")

	ch := make(chan commonstructs.SSEChannelStruct)
	go func() {
		openrouter.GetMachineResponse(body.Prompt, ch)
	}()
	for token := range ch {
		if token.Message == "" {
			continue
		}
		b, err := json.Marshal(token)
		if err != nil {
			continue
		}
		fmt.Fprintf(w, "data: %s\n\n", b)
		if ss, ok := w.(http.Flusher); ok {
			ss.Flush()
		}
	}
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
	HandlerFromMux(&Server{}, mux)

	addr := ":8080"
	log.Printf("leptos-agent service listening on %s", addr)
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatal(err)
	}
}
