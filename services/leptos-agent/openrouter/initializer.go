package openrouter

import (
	"errors"
	"os"
	"sync"

	openrouter "github.com/OpenRouterTeam/go-sdk"
)

var openRouterClient = sync.OnceValues(func() (*openrouter.OpenRouter, error) {
	key := os.Getenv("OPENROUTER_KEY")
	if key == "" {
		return nil, errors.New("OpenRouter key not set")
	}
	return openrouter.New(openrouter.WithSecurity(key)), nil
})
