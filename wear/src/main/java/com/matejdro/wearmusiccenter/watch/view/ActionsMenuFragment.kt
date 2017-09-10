package com.matejdro.wearmusiccenter.watch.view

import android.annotation.TargetApi
import android.app.Fragment
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.wear.widget.CurvingLayoutCallback
import android.support.wear.widget.WearableLinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import android.support.wearable.input.WearableButtons
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.matejdro.wearmusiccenter.R
import com.matejdro.wearmusiccenter.watch.config.ButtonAction


class ActionsMenuFragment : Fragment() {
    private lateinit var viewmodel: MusicViewModel

    private lateinit var recycler: WearableRecyclerView

    private var menuItems: List<ButtonAction> = emptyList()
    private lateinit var adapter: MenuAdapter
    private lateinit var layoutManager: MenuLayoutManager

    private var closeDrawerKeycode = -1

    private lateinit var activity: MainActivity

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        activity = context as MainActivity

        viewmodel = activity.viewModel
        viewmodel.actionsMenuConfig.config.observe(activity, configListener)

        findButtons()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        recycler = inflater.inflate(R.layout.fragment_actions_list, container, false)
                as WearableRecyclerView
        return recycler
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        adapter = MenuAdapter()

        layoutManager = MenuLayoutManager(context)
        recycler.layoutManager = layoutManager
        recycler.isEdgeItemsCenteringEnabled = true
        recycler.adapter = adapter
    }

    private val configListener = Observer<List<ButtonAction>> {
        if (it == null) {
            return@Observer
        }

        menuItems = it
        adapter.notifyDataSetChanged()
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun findButtons() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            return
        }

        val numButtons = WearableButtons.getButtonCount(context)
        if (numButtons <= 1) {
            // We have at most 1 button. Lets use that lone button for confirm action
            return
        }

        //Find button with lowest Y value (furthest from user) - that will be close button
        closeDrawerKeycode = (KeyEvent.KEYCODE_STEM_1..KeyEvent.KEYCODE_STEM_3)
                .map { WearableButtons.getButtonInfo(context, it) }
                .filterNotNull()
                .minBy { it.y }?.keycode ?: -1
    }

    fun scrollToTop() {
        recycler.scrollToPosition(0)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == closeDrawerKeycode) {
            (context as MainActivity).closeMenuDrawer()
        } else {
            viewmodel.executeActionFromMenu(layoutManager.getCenterItem())
        }
        return true
    }

    inner class MenuAdapter : RecyclerView.Adapter<MenuItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MenuItemViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_list_action, parent, false)
            return MenuItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
            val configItem = menuItems[position]

            holder.icon.setImageDrawable(configItem.icon)
            holder.title.text = configItem.title
        }

        override fun getItemCount(): Int = menuItems.size

    }

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val title: TextView = itemView.findViewById(R.id.text)

        init {
            itemView.tag = this
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            activity.buzz()
            viewmodel.executeActionFromMenu(adapterPosition)
        }
    }

    private inner class MenuLayoutManager(context: Context) : WearableLinearLayoutManager(context) {
        private val roundScreen: Boolean = resources.configuration.isScreenRound

        private var closestDistToCenter = Float.MAX_VALUE
        private var closestChildViewHolder: MenuItemViewHolder? = null

        fun getCenterItem(): Int {
            val closestChildViewHolder = closestChildViewHolder
                    ?: throw IllegalStateException("View not layouted yet")

            return closestChildViewHolder.adapterPosition
        }

        private fun prepareChildrenLayout() {
            closestDistToCenter = Float.MAX_VALUE
            closestChildViewHolder = null
        }

        private fun finishChildrenLayout() {
            for (childIndex in 0 until childCount) {
                val child = getChildAt(childIndex)
                val holder = child.tag as MenuItemViewHolder

                val textScale: Float
                val imageScale: Float
                if (holder == closestChildViewHolder) {
                    textScale = 1f
                    imageScale = 1f
                } else {
                    textScale = 0.5f
                    imageScale = 0.75f
                }

                child.alpha = textScale

                if (roundScreen) {
                    holder.icon.scaleX = imageScale
                    holder.icon.scaleY = imageScale
                }

            }
        }

        override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
            prepareChildrenLayout()
            val ret = super.scrollVerticallyBy(dy, recycler, state)
            finishChildrenLayout()
            return ret
        }

        override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
            prepareChildrenLayout()
            super.onLayoutChildren(recycler, state)
            finishChildrenLayout()
        }

        private val childLayoutCallback = object : CurvingLayoutCallback(context) {
            override fun onLayoutFinished(child: View, parent: RecyclerView?) {
                super.onLayoutFinished(child, parent)

                // Figure out % progress from top to bottom
                val centerOffset = child.height.toFloat() / 2.0f / recycler.height.toFloat()
                val yRelativeToCenterOffset = child.y / recycler.height + centerOffset

                // Normalize for center
                val progressToCenter = Math.abs(0.5f - yRelativeToCenterOffset)

                val holder = child.tag as MenuItemViewHolder

                if (closestDistToCenter > progressToCenter) {
                    closestChildViewHolder = holder
                    closestDistToCenter = progressToCenter
                }
            }

        }

        init {
            layoutCallback = childLayoutCallback
        }
    }


}