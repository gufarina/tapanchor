package com.tapanchor.app.hw;

oneway interface IKeyListener {
    void onKey(int code, boolean down);
    void onGrab(boolean grabbed);
}
