//
//  ModelSelection.swift
//  iosApp
//
//  Created by florida on 14/11/26.
//

import SwiftUI
import Shared

struct ModelSelection : View {
    
    enum Effort : Double, CaseIterable {
        case low = 0.0
        case medium = 0.25
        case high = 0.5
        case `default` = 0.75
        case max = 1.0
        
        var label: String {
            switch self {
            case .low: "low"
            case .medium: "medium"
            case .high: "high"
            case .`default`: "default"
            case .max: "max"
            }
        }
        
        init(dbValue: String) {
            let normalized = dbValue.lowercased().trimmingCharacters(in: .whitespaces)
            self = Effort.allCases.first { $0.label == normalized } ?? .`default`
        }
    }
    
    private var modelList : [String] = ["GLM-5.3-lite", "GLM-5.4"]
    private var providerList : [String] = ["openrouter","anthropic","openai"]
    
    //breakdown of the user selections
    @State private var provider : String = "openrouter"
    @State private var model : String = "GLM-5.3-lite"
    @State private var effortSelected : String = "default"  // DB-backed source of truth
    @State private var isOnlineMode : Bool = false
    
    
    private var effort : Effort {
        get { Effort(dbValue: effortSelected) }
        nonmutating set { effortSelected = newValue.label }
    }
    
    
    private func persistPrefs() {
        saveModelPrefs(
            userId: "Self.localUserId",          // ← see note below
            modelProvider: provider,
            model: model,
            effort: effortSelected,
            useOnlineLeptos: isOnlineMode
        )
    }
    
    var body : some View {
        VStack {
            VStack() {
                Form {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(alignment: .center) {
                            Text("Providers & Models")
                                .font(.system(size: 22, weight: .bold))
                            Spacer()
                            // ProgressView()
                            // .progressViewStyle(.circular)
                            // .controlSize(.small)
                            Text("saved")
                                .font(.subheadline)
                                .foregroundStyle(.blue)
                        }
                        Text(
                            "Select the Provider and related model. You can switch between on-device BYOK or use free limited time model usage."
                        )
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineSpacing(3)
                    }
                    Picker("Provider", selection: $provider) {
                        ForEach(providerList, id: \.self) {flavor in
                            Text(flavor.capitalized)
                                .tag(flavor)
                        }
                    }
                    .onChange(of: provider){_,_ in persistPrefs()}
                    Picker("Model", selection: $model) {
                        ForEach(modelList, id: \.self) {model in
                            Text(model)
                        }
                    }
                    .onChange(of: model){_,_ in persistPrefs()}
                    VStack {
                        HStack {
                            Text("Thinking effort")
                            Spacer()
                            Text(effort.label.capitalized)
                                .foregroundStyle(.secondary)
                        }
                        Slider(value: Binding(
                            get: { effort.rawValue },
                            set: { newValue, transacting in
                                effort = Effort.allCases.min(
                                    by: { abs($0.rawValue - newValue) < abs($1.rawValue - newValue) }
                                ) ?? .medium
                                if !transacting.isContinuous { persistPrefs() }
                            }
                        )) {
                            Text("Effort")
                        } currentValueLabel: {
                            Text(effort.label)
                        } ticks: {
                            SliderTickContentForEach(
                                Effort.allCases.map(\.rawValue),
                                id: \.self
                            ) { value in
                                SliderTick(value) {
                                }
                            }
                        }
                    }
                    Toggle(isOn: $isOnlineMode){
                        Text("Use Leptos online model")
                    }
                    .onChange(of: isOnlineMode){_,_ in persistPrefs()}
                }
            }
            .onAppear{
                UserPrefsRead().observeAll{list in
                    if(list.count>0){
                        provider = list[0].modelProvider
                        model = list[0].model
                        effortSelected = list[0].effortLevel
                        isOnlineMode = list[0].useOnlineLeptos
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
