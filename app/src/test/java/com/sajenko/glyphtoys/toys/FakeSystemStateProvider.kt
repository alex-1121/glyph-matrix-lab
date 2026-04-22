package com.sajenko.glyphtoys.toys

class FakeSystemStateProvider : SystemStateProvider {
    override var isCallActive: Boolean = false
    override var isMediaPlaying: Boolean = false
}
