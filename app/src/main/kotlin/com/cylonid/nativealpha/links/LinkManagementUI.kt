package com.cylonid.nativealpha.links

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Dialog for link management UI
 */
class LinkManagementDialog(
    private val linkSystem: LinkManagementSystem,
    private val onLinkSelected: (url: String, format: LinkManagementSystem.LinkFormat) -> Unit
) : DialogFragment() {

    private var selectedFormat = LinkManagementSystem.LinkFormat.PLAIN_URL

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext()).apply {
            setTitle("Link Management")
            setView(createDialogView())
            setNegativeButton("Cancel") { _, _ -> dismiss() }
        }.create()
    }

    private fun createDialogView(): android.view.View {
        val container = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 20, 20, 20)
            }
        }

        // Format selection
        val formatLabel = TextView(requireContext()).apply {
            text = "Copy Format:"
            textSize = 14f
            setTextAppearance(android.R.style.TextAppearance_Medium)
        }
        mainLayout.addView(formatLabel)

        val formatSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                LinkManagementSystem.LinkFormat.values().map { it.name }
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, p2: Int, p3: Long) {
                    selectedFormat = LinkManagementSystem.LinkFormat.values()[p2]
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
        mainLayout.addView(formatSpinner)

        // Preview
        val previewLabel = TextView(requireContext()).apply {
            text = "Preview:"
            textSize = 14f
            setTextAppearance(android.R.style.TextAppearance_Medium)
            setPadding(0, 20, 0, 10)
        }
        mainLayout.addView(previewLabel)

        val previewText = EditText(requireContext()).apply {
            isEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                120
            )
            hint = "Preview will appear here"
        }
        mainLayout.addView(previewText)

        // Saved links
        val savedLabel = TextView(requireContext()).apply {
            text = "Saved Links:"
            textSize = 14f
            setTextAppearance(android.R.style.TextAppearance_Medium)
            setPadding(0, 20, 0, 10)
        }
        mainLayout.addView(savedLabel)

        val linksList = ListView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            linkSystem.savedLinks.collect { links ->
                val adapter = LinkListAdapter(requireContext(), links) { link ->
                    dismiss()
                    onLinkSelected(link.url, link.format)
                }
                linksList.adapter = adapter
            }
        }
        mainLayout.addView(linksList)

        container.addView(mainLayout)
        return container
    }

    private inner class LinkListAdapter(
        context: Context,
        private val links: List<LinkManagementSystem.SavedLink>,
        private val onLinkClick: (LinkManagementSystem.SavedLink) -> Unit
    ) : BaseAdapter() {

        private val inflater = LayoutInflater.from(context)

        override fun getCount() = links.size

        override fun getItem(position: Int) = links[position]

        override fun getItemId(position: Long) = position

        override fun getView(position: Int, convertView: android.view.View?, parent: ViewGroup?): android.view.View {
            val view = convertView ?: inflater.inflate(
                android.R.layout.simple_list_item_2,
                parent,
                false
            )

            val link = links[position]
            view.findViewById<TextView>(android.R.id.text1).text = link.pageTitle
            view.findViewById<TextView>(android.R.id.text2).text = link.url

            view.setOnClickListener {
                onLinkClick(link)
            }

            return view
        }
    }
}

/**
 * Link picker for selecting from saved links or formats
 */
class LinkPickerBottomSheet(
    private val linkSystem: LinkManagementSystem,
    private val onLinkSelected: (String, LinkManagementSystem.LinkFormat) -> Unit
) : androidx.fragment.app.BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Quick format buttons
            addView(createFormatButton("Plain URL", LinkManagementSystem.LinkFormat.PLAIN_URL))
            addView(createFormatButton("With Title", LinkManagementSystem.LinkFormat.URL_WITH_TITLE))
            addView(createFormatButton("Markdown", LinkManagementSystem.LinkFormat.MARKDOWN))
            addView(createFormatButton("HTML", LinkManagementSystem.LinkFormat.HTML_ANCHOR))
        }
    }

    private fun createFormatButton(
        text: String,
        format: LinkManagementSystem.LinkFormat
    ): android.view.View {
        return Button(requireContext()).apply {
            setText(text)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
            setOnClickListener {
                // Trigger copy with selected format
                dismiss()
            }
        }
    }
}
