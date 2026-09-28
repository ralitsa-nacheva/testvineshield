import SwiftUI
import shared

@main
struct iOSApp: App {

init() {
        KoinInitIosKt.initKoinIos()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}