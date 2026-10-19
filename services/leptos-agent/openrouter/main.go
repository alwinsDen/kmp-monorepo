package openrouter

import (
	"context"

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
	}, nil)
	if err != nil {
		return nil, err
	}
	if res != nil {
		if c, ok := res.ChatResult.Choices[0].Message.Content.Get(); ok && c.Str != nil {
			return c.Str, nil
		}
	}
	return nil, nil
}
