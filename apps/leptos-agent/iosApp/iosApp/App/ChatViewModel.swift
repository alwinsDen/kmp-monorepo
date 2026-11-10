import Foundation
import Shared

@MainActor
final class ChatViewModel: ObservableObject {
    private static let apiBaseUrl: String = "http://172.20.10.9:8080"
    private let api = ApiClient(baseUrl: apiBaseUrl)
    
    // Reveal pacing: type out text at a readable base speed, but accelerate when
    // tokens arrive faster than they are revealed so we never fall far behind.
    private static let revealFPS: Double = 30
    private static let revealBaseSpeed: Double = 60
    private static let revealCatchUpWindow: Double = 0.8            // seconds of backlog tolerated
    
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
    // Typewriter reveal state for the message currently streaming in.
    @Published private(set) var streamingMessageID: ChatMessage.ID?
    /// Rolling reveal positions, newest first. The view renders the text up to
    /// each position as a stacked fade-in layer.
    @Published private(set) var revealWindow: [Int] = [0, 0, 0]
    private var revealedCount = 0
    private var streamFinished = false
    private var revealTask: Task<Void, Never>?
    
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
                        let chunk = token.message ?? ""
                        self.transcriptResponse += chunk
                        self.chatArray[botResponseIndex].text += chunk
                        if self.streamingMessageID == nil {
                            self.startReveal(for: self.chatArray[botResponseIndex].id)
                        }
                    }
                },
                requestPassed: {
                    DispatchQueue.main.async {
                        self.sendPhaseState = .streaming
                    }
                }
            )
            streamFinished = true
        } catch {
            print("stream failed: \(error)")
            streamFinished = true
        }
    }
    
    // MARK: - Typewriter reveal
    
    /// Starts the typewriter loop that paces out newly streamed text.
    private func startReveal(for messageID: ChatMessage.ID) {
        streamingMessageID = messageID
        revealedCount = 0
        revealWindow = [0, 0, 0]
        streamFinished = false
        revealTask?.cancel()
        revealTask = Task {
            while !Task.isCancelled {
                let target = revealedTargetCount()
                if revealedCount < target {
                    // Advance this tick; speed up when a backlog builds up.
                    let backlog = Double(target - revealedCount)
                    let speed = max(Self.revealBaseSpeed, backlog / Self.revealCatchUpWindow)
                    let step = max(1, Int((speed / Self.revealFPS).rounded(.up)))
                    revealedCount = min(target, revealedCount + step)
                } else if streamFinished {
                    // Fully revealed and the network stream is done: hand the
                    // message back to plain rendering.
                    streamingMessageID = nil
                    revealedCount = 0
                    revealWindow = [0, 0, 0]
                    break
                }
                // Roll the fade window so the newest characters render faintest.
                let window = [revealedCount, revealWindow[0], revealWindow[1]]
                if window != revealWindow {
                    revealWindow = window
                }
                try? await Task.sleep(for: .milliseconds(33))
            }
        }
    }
    
    private func revealedTargetCount() -> Int {
        guard let id = streamingMessageID,
              let index = chatArray.firstIndex(where: { $0.id == id }) else { return 0 }
        return chatArray[index].text.count
    }
}
