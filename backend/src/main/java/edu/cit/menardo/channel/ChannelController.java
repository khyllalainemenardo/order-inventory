package edu.cit.menardo.channel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SuppressWarnings("unused")
class ChannelController {

    private final SalesChannel channel;

    ChannelController(SalesChannel channel) {
        this.channel = channel;
    }

    @GetMapping("/api/channel/status")
    ChannelStatus status() {
        return channel.status();
    }
}
