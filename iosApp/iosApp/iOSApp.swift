import SwiftUI
import Shared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        KoinInitIosKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup { ContentView() }
            .onChange(of: scenePhase, initial: true) { _, phase in
                switch phase {
                case .active: IosAppLifecycle.shared.onForeground()
                case .inactive: IosAppLifecycle.shared.onInactive()
                case .background: IosAppLifecycle.shared.onBackground()
                @unknown default: IosAppLifecycle.shared.onInactive()
                }
            }
    }
}
