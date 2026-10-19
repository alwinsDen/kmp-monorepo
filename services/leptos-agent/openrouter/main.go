package openrouter

import (
	"context"
	"errors"
	"strings"

	"github.com/OpenRouterTeam/go-sdk/models/components"
)

var (
	glm5_3_flash string = "z-ai/glm-5.3-flash"
	glm5_3       string = "z-ai/glm-5.3"
)

func GetMachineResponse(userQuery string) (*string, error) {
	ctx_start := context.Background()
	client, err := openRouterClient()
	if err != nil {
		return nil, err
	}
	res, err := client.Chat.Send(ctx_start, components.ChatRequest{
		Models: []string{glm5_3_flash},
		Messages: []components.ChatMessages{
			components.CreateChatMessagesUser(
				components.ChatUserMessage{
					Content: components.CreateChatUserMessageContentStr(userQuery),
					Role:    components.ChatUserMessageRoleUser,
				},
			),
		},
		Stream: new(true),
	}, nil)
	if err != nil {
		return nil, err
	}
	var tokenCollector strings.Builder
	if res != nil {
		es := res.EventStream
		defer es.Close()
		for es.Next() {
			chunk := *es.Value()
			if chunk.Data.Error != nil {
				return nil, errors.New(chunk.Data.Error.Message)
			}
			for _, choice := range chunk.Data.Choices {
				if c, ok := choice.Delta.Content.Get(); ok {
					tokenCollector.WriteString(*c)
				}
			}
		}
	}
	return new(tokenCollector.String()), nil
}
