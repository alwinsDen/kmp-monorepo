import Shared
import SwiftUI

struct ContentView: View {
    @StateObject private var chatViewModel = ChatViewModel()
    @State private var userQuery: String = ""
    @State private var showLeptosWork = false
    @State private var showContent = false

    var body: some View {
        VStack {
            ScrollView(){
                HStack {
                    Spacer()
                    VStack{
                        Text("hi")
                    }.padding(16)
                        .background(.ultraThinMaterial, in: .rect(cornerRadius: 24))
                }
                .padding(.trailing, 16)
                VStack{
                    if(chatViewModel.sendPhaseState == .connecting){
                        HStack {
                            RunLottie(fileName: "loader_cat", size: 64)
                            Text("leptos at work...")
                            Spacer()
                        }
                    } else {
                        Spacer().frame(height: 10)
                    }
                    Text(chatViewModel.transcriptResponse)
                        .padding(.vertical, 0)
                }
            }
            .ignoresSafeArea(.keyboard, edges: .bottom)
            .scrollDismissesKeyboard(.immediately)
            .padding(.leading, 10)
            VStack {
                TextField("Ask Leptos agent...", text: $userQuery, axis: .vertical)
                    .lineLimit(6)
                    .multilineTextAlignment(.leading)
                Spacer().frame(height: 20)
                HStack {
                    Menu {
                        Button {
                            print("C:camera-lock")
                        } label: { Label("Camera", systemImage: "camera") }
                        Button { } label: { Label("Photos", systemImage: "photo") }
                        Button { } label: { Label("Files", systemImage: "paperclip") }
                        Button { } label: { Label("Plugins", systemImage: "at") }
                    } label: {
                        SharedIcon(systemName: "plus.circle.fill")
                    }.labelStyle(.iconOnly)
                        .buttonStyle(.plain)
                    Spacer().frame(width: 20)
                    Button {
                        //
                    }label: {
                        Text("Leptos Basic")
                            .foregroundStyle(Color.primary.opacity(0.8))
                            .padding(6)
                    }
                    .background(.ultraThinMaterial, in: .rect(cornerRadius: 8))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.primary.opacity(0.8), lineWidth: 0.5)
                    )
                    Spacer()
                    Button{
                        //
                    } label: {
                        SharedIcon(systemName: "music.microphone.circle.fill")
                    }
                    .labelStyle(.iconOnly)
                    .buttonStyle(.plain)
                    Spacer().frame(width: 20)
                    Button{
                        Task { await chatViewModel.send(prompt: userQuery) }
                    } label: {
                        SharedIcon(systemName: "paperplane.circle.fill")
                    }
                    .labelStyle(.iconOnly)
                    .buttonStyle(.plain)
                }
            }.padding(12)
                .background(.ultraThinMaterial, in: .rect(cornerRadius: 14))
                .padding(.horizontal, 6)
                .padding(.bottom, 12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .task {
            await chatViewModel.checkHealth()
            print("backend health: \(chatViewModel.healthStatus)")
        }
    }
}
