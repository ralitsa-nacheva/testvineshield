import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    let linkReceiver: IosVineyardLinkReceiver

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(linkReceiver: linkReceiver)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var linkReceiver = IosVineyardLinkReceiver()

    var body: some View {
        ComposeView(linkReceiver: linkReceiver)
            .ignoresSafeArea()
            .onOpenURL { url in
                linkReceiver.receive(url: url.absoluteString)
            }
    }
}
