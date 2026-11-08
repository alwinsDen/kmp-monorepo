//
//  Navigation.swift
//  iosApp
//
//  Created by florida on 10/09/26.
//
import SwiftUI

struct NavigationController: View {
    var body: some View {

        TabView {
            Tab("New Chat", systemImage: "plus.app.fill") {
                NavigationStack {
                    ContentView()
                }
            }
            Tab("History", systemImage: "wallet.pass.fill") {
                NavigationStack {
                    //
                }
            }
            Tab("Settings", systemImage: "gear") {
                NavigationStack {
                    //
                }
            }
        }
    }
}
