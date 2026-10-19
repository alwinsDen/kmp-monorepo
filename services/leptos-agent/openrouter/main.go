package openrouter

import (
	"context"

	"github.com/OpenRouterTeam/go-sdk/models/components"
	commonstructs "github.com/alwinsden/kmp-monorepo/services/commonStructs"
)

var (
	glm5_3_flash string = "z-ai/glm-5.3-flash"
	glm5_3       string = "z-ai/glm-5.3"
)

func GetMachineResponse(userQuery string, channel chan commonstructs.SSEChannelStruct) {
	ctx_start := context.Background()
	client, err := openRouterClient()
	if err != nil {
		channel <- commonstructs.SSEChannelStruct{
			Message: err.Error(),
			Type:    "FAILURE",
		}
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
		channel <- commonstructs.SSEChannelStruct{
			Message: err.Error(),
			Type:    "FAILURE",
		}
	}
	if res != nil {
		es := res.EventStream
		defer es.Close()
		for es.Next() {
			chunk := *es.Value()
			if chunk.Data.Error != nil {
				channel <- commonstructs.SSEChannelStruct{
					Message: chunk.Data.Error.Message,
					Type:    "FAILURE",
				}
			}
			for _, choice := range chunk.Data.Choices {
				if c, ok := choice.Delta.Content.Get(); ok {
					channel <- commonstructs.SSEChannelStruct{
						Message: *c,
						Type:    "BOT",
					}
				}
			}
		}
		close(channel)
	}
}
