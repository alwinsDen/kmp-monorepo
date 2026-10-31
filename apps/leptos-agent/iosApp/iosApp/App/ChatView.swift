import Shared
import SwiftUI
import Textual

struct ContentView: View {
    @StateObject private var chatViewModel = ChatViewModel()
    @State private var userQuery: String = ""
    @State private var showLeptosWork = false
    @State private var showContent = false
    @FocusState private var isChatInputInFocus: Bool
    
    private func scrollToBottom(_ proxy: ScrollViewProxy) {
        if let last = chatViewModel.chatArray.last {
            withAnimation(.easeOut(duration: 0.15)) {
                proxy.scrollTo(last.id, anchor: .bottom)
            }
        }
    }

    var body: some View {
        VStack {
            ScrollViewReader{proxy in
                ScrollView(){
                    LazyVStack {
                        ForEach(chatViewModel.chatArray) {arrayDetails in
                            if(arrayDetails.isUser){
                                HStack {
                                    Spacer()
                                    VStack{
                                        Text(arrayDetails.text)
                                    }.padding(16)
                                    .background(.ultraThinMaterial, in: .rect(cornerRadius: 24))
                                }.id(arrayDetails.id)
                                .padding(.trailing, 16)
                                .padding(.bottom, 8)
                            }
                            if(!chatViewModel.transcriptResponse.isEmpty && !arrayDetails.isUser){
                                VStack{
                                    StructuredText(markdown:arrayDetails.text)
                                }.frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(.horizontal, 12)
                                    .padding(.bottom, 8)
                            }
                        }
                    }
                    if(chatViewModel.sendPhaseState == .connecting){
                        HStack {
    //                        RunLottie(fileName: "loader_cat", size: 64)
    //                        Text("leptos at work...")
    //                        Spacer()
                            ProgressView()
                                .progressViewStyle(.circular)
                                .controlSize(.regular)
                            Text("leptos at work...")
                                .phaseAnimator([0.15,1.0,1.0]){text, opacity in
                                    text.opacity(opacity)
                                } animation: { _ in
                                    .easeInOut(duration: 0.7)
                                }
                            Spacer()
                        }.padding(.vertical, 0)
                            .padding(.horizontal, 12)
                    }
                }
                .ignoresSafeArea(.keyboard, edges: .bottom)
                .scrollDismissesKeyboard(.immediately)
                .padding(.leading, 10)
                .onChange(of: chatViewModel.chatArray.count){_,_ in
                    scrollToBottom(proxy)
                }
//                .onChange(of: chatViewModel.chatArray.last?.text) {_,_ in
//                    scrollToBottom(proxy)
//                }
            }
            VStack {
                TextField("Ask Leptos agent...", text: $userQuery, axis: .vertical)
                    .lineLimit(6)
                    .multilineTextAlignment(.leading)
                    .focused($isChatInputInFocus)
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
                        isChatInputInFocus = false
                        let prompt = userQuery
                        userQuery = ""
                        Task {
                            await chatViewModel.send(prompt: prompt)
                        }
                    } label: {
                        SharedIcon(systemName: "paperplane.circle.fill")
                    }
                    .labelStyle(.iconOnly)
                    .buttonStyle(.plain)
                }
            }.padding(12)
                .background(.ultraThinMaterial, in: .rect(cornerRadius: 14))
                .padding(.horizontal, 15)
                .padding(.bottom, 12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .task {
            await chatViewModel.checkHealth()
            print("backend health: \(chatViewModel.healthStatus)")
        }
    }
}
