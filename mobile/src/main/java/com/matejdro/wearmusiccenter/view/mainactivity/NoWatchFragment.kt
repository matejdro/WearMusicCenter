package com.matejdro.wearmusiccenter.view.mainactivity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.view.TitledActivity

class NoWatchFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_no_watch, container, false)
    }

    override fun onStart() {
        super.onStart()

        val activity = activity
        if (activity is TitledActivity) {
            activity.title = getString(R.string.app_name_short)
        }
    }
}