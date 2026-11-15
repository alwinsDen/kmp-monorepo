//
//  ModelSelection.swift
//  iosApp
//
//  Created by florida on 14/11/26.
//

import SwiftUI

struct ModelSelection : View {
    enum Provider : String, CaseIterable, Identifiable {
        case anthropic, openrouter, openAI
        var id : Self { self }
    }
    
    enum Effort : Double, CaseIterable {
        case low = 0.0
        case medium = 0.25
        case high = 0.5
        case xhigh = 0.75
        case max = 1.0
        
        var label: String {
            switch self {
            case .low: "Low"
            case .medium: "Medium"
            case .high: "High"
            case .xhigh: "XHigh"
            case .max: "Max"
            }
        }
    }
    
    @State private var selectedProvider : Provider = .openrouter
    @State private var selectedModel: String = "GLM-5.3-lite"
    @State private var modelList : [String] = ["GLM-5.3-lite", "GLM-5.4"]
    @State private var effort : Effort = .medium
    
    var body : some View {
        VStack {
            HStack {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 44))
                    .frame(width: 44, height: 44)
                    .symbolRenderingMode(.multicolor)
                    .foregroundStyle(.white)
                Spacer()
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 44))
                    .frame(width: 44, height: 44)
                    .symbolRenderingMode(.multicolor)
                    .foregroundStyle(.white)
            }.padding(12)
            VStack() {
                Form {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Providers & Models")
                            .font(.system(size: 22, weight: .bold))
                        Text("Select the Provider and related model. You can switch between on-device BYOK or use free limited time model usage.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .lineSpacing(3)
                    }
                    Picker("Provider", selection: $selectedProvider) {
                        ForEach(Provider.allCases) {flavor in
                            Text(flavor.rawValue.capitalized)
                                .tag(flavor)
                        }
                    }
                    Picker("Model", selection: $selectedModel) {
                        ForEach(modelList, id: \.self) {model in
                            Text(model)
                        }
                    }
                    VStack {
                        HStack {
                            Text("Select Thinking effort")
                                .foregroundStyle(.secondary)
                            Spacer()
                            Text(effort.label)
                        }
                        Slider(value: Binding(
                            get: { effort.rawValue },
                            set: { newValue, _ in
                                effort = Effort.allCases.min(
                                    by: { abs($0.rawValue - newValue) < abs($1.rawValue - newValue) }
                                ) ?? .medium
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
                }
            }
        }
        .presentationDetents([.medium])
    }
}
