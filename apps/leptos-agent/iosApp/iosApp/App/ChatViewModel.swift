import Foundation
import Shared

@MainActor
final class ChatViewModel: ObservableObject {
    private static let apiBaseUrl: String = "http://192.168.11.198:8080"
    private let api = ApiClient(baseUrl: apiBaseUrl)

    @Published var healthStatus: String = ""
    @Published var isWorking = false
    @Published var transcriptResponse = ""

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
            let response = try await api.submitConversationStream(
                request: ConversationSubmitRequest(model: "leptos-basic", prompt: prompt)
            ) { token in
                DispatchQueue.main.async {
                    self.transcriptResponse += token.message ?? ""
                }
            }
        } catch {
            print("stream failed: \(error)")
        }
    }
}
