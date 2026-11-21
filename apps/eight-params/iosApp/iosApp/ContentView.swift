import SwiftUI
import Shared

struct ContentView: View {
    var body: some View {
        Text(Greeting().greet())
            .font(.headline)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    ContentView()
}
