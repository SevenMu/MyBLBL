package com.myblbl.ui.fragment.main

interface MainTabFocusTarget {
    fun focusEntryFromMainTab(): Boolean

    fun focusEntryFromMainTab(anchorView: android.view.View?, preferSpatialEntry: Boolean): Boolean {
        return focusEntryFromMainTab()
    }
}
