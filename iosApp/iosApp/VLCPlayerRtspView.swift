import UIKit
import MobileVLCKit
import Shared
import Darwin

final class VLCPlayerRtspView: UIView, VLCMediaPlayerDelegate {
    private var mediaPlayer: VLCMediaPlayer?
    private let urlString: String
    private let isLiveStream: Bool
    private let onStateChanged: (RtspPlayerState) -> Void
    private var isPlayerStarted = false
    private var hasReportedPlaying = false
    private let videoView = UIView()

    init(url: String, isLiveStream: Bool = true, onStateChanged: @escaping (RtspPlayerState) -> Void) {
        self.urlString = url
        self.isLiveStream = isLiveStream
        self.onStateChanged = onStateChanged
        super.init(frame: .zero)

        backgroundColor = .black
        clipsToBounds = true
        videoView.backgroundColor = .black
        videoView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        addSubview(videoView)

        if !isLiveStream {
            let tap = UITapGestureRecognizer(target: self, action: #selector(togglePlayPause))
            addGestureRecognizer(tap)
        }
    }

    @objc private func togglePlayPause() {
        guard let player = mediaPlayer else { return }
        if player.isPlaying {
            player.pause()
        } else if player.state == .ended || player.state == .stopped {
            if let streamUrl = URL(string: urlString) {
                player.media = VLCMedia(url: streamUrl)
            }
            player.play()
        } else {
            player.play()
        }
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        videoView.frame = bounds
        videoView.subviews.forEach { $0.frame = bounds }
        videoView.layer.sublayers?.forEach { $0.frame = bounds }

        if !isPlayerStarted && bounds.width > 0 && bounds.height > 0 {
            isPlayerStarted = true
            setupAndStartPlayer()
        }
    }

    private func setupAndStartPlayer() {
        guard let streamUrl = URL(string: urlString) else {
            onStateChanged(RtspPlayerState.error)
            return
        }

        if isLiveStream, let host = streamUrl.host {
            configureLive555Interface(forHost: host)
        }

        let library = isLiveStream ? VLCLibrary(options: ["--no-audio"]) : VLCLibrary()
        library.debugLogging = false

        let player = VLCMediaPlayer(library: library)
        player.drawable = videoView
        player.delegate = self

        let media = VLCMedia(url: streamUrl)
        if isLiveStream {
            media.addOption(":network-caching=200")
            media.addOption(":live-caching=200")
            media.addOption(":clock-jitter=0")
            media.addOption(":clock-synchro=0")
            media.addOption(":no-audio")
        }

        player.media = media
        player.play()
        self.mediaPlayer = player
    }

    private func configureLive555Interface(forHost host: String) {
        guard let localIp = getLocalIp(for: host) else { return }
        var addr = in_addr()
        guard inet_aton(localIp, &addr) != 0 else { return }

        let handle = UnsafeMutableRawPointer(bitPattern: -2)
        for symbol in ["ReceivingInterfaceAddr", "SendingInterfaceAddr"] {
            if let ptr = dlsym(handle, symbol) {
                ptr.assumingMemoryBound(to: UInt32.self).pointee = addr.s_addr
            }
        }
    }

    private func getLocalIp(for host: String) -> String? {
        var hints = addrinfo()
        hints.ai_family = AF_INET
        hints.ai_socktype = SOCK_DGRAM
        var res: UnsafeMutablePointer<addrinfo>?

        if getaddrinfo(host, "554", &hints, &res) == 0, let serverAddr = res {
            defer { freeaddrinfo(res) }
            let sock = socket(AF_INET, SOCK_DGRAM, 0)
            if sock >= 0 {
                defer { close(sock) }
                var localAddr = sockaddr_in()
                var addrLen = socklen_t(MemoryLayout<sockaddr_in>.size)
                let connected = connect(sock, serverAddr.pointee.ai_addr, serverAddr.pointee.ai_addrlen) == 0
                let bound = withUnsafeMutablePointer(to: &localAddr) { ptr in
                    ptr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sockPtr in
                        getsockname(sock, sockPtr, &addrLen) == 0
                    }
                }
                if connected && bound {
                    var buffer = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
                    inet_ntop(AF_INET, &localAddr.sin_addr, &buffer, socklen_t(INET_ADDRSTRLEN))
                    let ip = String(cString: buffer)
                    if !ip.isEmpty && ip != "0.0.0.0" {
                        return ip
                    }
                }
            }
        }

        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0, let firstAddr = ifaddr else { return nil }
        defer { freeifaddrs(ifaddr) }

        for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next }) {
            guard let addr = ptr.pointee.ifa_addr, addr.pointee.sa_family == UInt8(AF_INET) else { continue }
            let name = String(cString: ptr.pointee.ifa_name)
            if name.hasPrefix("en") || name.hasPrefix("ap") || name.hasPrefix("bridge") {
                var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                if getnameinfo(addr, socklen_t(addr.pointee.sa_len), &hostname, socklen_t(hostname.count), nil, 0, NI_NUMERICHOST) == 0 {
                    let ip = String(cString: hostname)
                    if !ip.hasPrefix("127.") && !ip.hasPrefix("169.254.") {
                        return ip
                    }
                }
            }
        }
        return nil
    }

    func mediaPlayerStateChanged(_ aNotification: Notification) {
        updatePlaybackState()
    }

    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        if !hasReportedPlaying && (mediaPlayer?.videoSize.width ?? 0) > 0 {
            updatePlaybackState()
        }
    }

    private func updatePlaybackState() {
        guard let player = mediaPlayer else { return }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            if player.videoSize.width > 0 {
                if !self.hasReportedPlaying {
                    self.hasReportedPlaying = true
                    self.onStateChanged(RtspPlayerState.playing)
                }
                return
            }

            switch player.state {
            case .opening, .buffering:
                if !self.hasReportedPlaying {
                    self.onStateChanged(RtspPlayerState.buffering)
                }
            case .playing:
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                    self?.updatePlaybackState()
                }
            case .error:
                self.hasReportedPlaying = false
                self.onStateChanged(RtspPlayerState.error)
            case .stopped, .ended, .paused:
                self.hasReportedPlaying = false
                self.onStateChanged(RtspPlayerState.idle)
            @unknown default:
                break
            }
        }
    }

    func stopAndRelease() {
        let player = mediaPlayer
        mediaPlayer = nil
        isPlayerStarted = false
        hasReportedPlaying = false

        player?.drawable = nil
        player?.delegate = nil

        DispatchQueue.global(qos: .userInitiated).async {
            player?.stop()
        }
    }
}

final class VLCPlayerViewProvider: NativeRtspViewProvider {
    func createView(url: String, onStateChanged: @escaping (RtspPlayerState) -> Void) -> UIView {
        VLCPlayerRtspView(url: url, isLiveStream: true, onStateChanged: onStateChanged)
    }

    func createVideoView(url: String, onStateChanged: @escaping (RtspPlayerState) -> Void) -> UIView {
        VLCPlayerRtspView(url: url, isLiveStream: false, onStateChanged: onStateChanged)
    }

    func releaseView(view: UIView) {
        (view as? VLCPlayerRtspView)?.stopAndRelease()
    }
}
