import Foundation
import Shared

@MainActor
final class ChatViewModel: ObservableObject {
    private static let apiBaseUrl: String = "http://172.20.10.9:8080"
    private let api = ApiClient(baseUrl: apiBaseUrl)
    
    enum sendPhase {case idle, connecting, streaming}
    struct ChatMessage : Identifiable {
        let id = UUID()
        let isUser : Bool
        var text : String
    }

    @Published var healthStatus: String = ""
    @Published var transcriptResponse = ""
    @Published var sendPhaseState : sendPhase = .idle
    @Published var chatArray : [ChatMessage] = []

    func checkHealth() async {
        do {
            let health = try await api.getHealth()
            healthStatus = health.status
        } catch {
            healthStatus = "unreachable: \(error.localizedDescription)"
        }
    }

    func send(prompt: String) async {
        guard !prompt.isEmpty else { return }
        self.sendPhaseState = .connecting
        defer { self.sendPhaseState = .idle }
        
        //chat array building logic
        chatArray.append(ChatMessage(isUser: true, text: prompt))
        chatArray.append(ChatMessage(isUser: false, text: ""))
        let botResponseIndex = chatArray.count - 1
        
        do {
            let response = try await api.submitConversationStream(
                request: ConversationSubmitRequest(model: "leptos-basic", prompt: prompt),
                onToken: { token in
                    DispatchQueue.main.async {
                        self.transcriptResponse += token.message ?? ""
                        self.chatArray[botResponseIndex].text += token.message ?? ""
                    }
                },
                requestPassed: {
                    DispatchQueue.main.async {
                        self.sendPhaseState = .streaming
                    }
                }
            )
        } catch {
            print("stream failed: \(error)")
        }
    }
}
