package com.myblbl.feature.live

import com.myblbl.core.ui.focus.TabContentFocusTarget

interface LiveTabPage : TabContentFocusTarget {
    fun scrollToTop()

    fun onTabSelected() {}

    fun onExplicitRefresh() {
        onReselected()
    }

    fun onReselected() {}
}
