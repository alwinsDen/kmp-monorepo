import Foundation
import Shared

@MainActor
final class ChatViewModel: ObservableObject {
    private static let apiBaseUrl : String = "http://localhost:8080"
    private let api = ApiClient(baseUrl: apiBaseUrl)

    @Published var healthStatus: String = ""
    @Published var isWorking = false

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
        isWorking = true
        defer { isWorking = false }
        do {
            let response = try await api.submitConversation(
                request: ConversationSubmitRequest(model: "leptos-basic", prompt: prompt)
            )
            print("conversation \(response.conversationId): \(response.message)")
        } catch {
            print("submit failed: \(error)")
        }
    }
}
