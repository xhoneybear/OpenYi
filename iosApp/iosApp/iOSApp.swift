import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        NativeRtspPlayerRegistry.shared.provider = VLCPlayerViewProvider()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
