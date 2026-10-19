package commonstructs

var ApiTypes = struct {
	GET  string
	POST string
}{GET: "GET", POST: "POST"}

type SSEChannelStruct struct {
	Message string `json:"message"`
	Type    string `json:"type"`
}
