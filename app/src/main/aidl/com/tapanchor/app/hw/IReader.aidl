package com.tapanchor.app.hw;

import com.tapanchor.app.hw.IKeyListener;

interface IReader {
    void destroy() = 16777114;
    void start(String deviceName, IKeyListener listener) = 1;
    void stop() = 2;
    void setBindings(String serialized) = 3;
    void setPaused(boolean paused) = 4;
    void allowOverlay(String pkg) = 5;
}
