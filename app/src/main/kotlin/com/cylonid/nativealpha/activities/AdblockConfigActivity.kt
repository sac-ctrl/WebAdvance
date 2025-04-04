package com.cylonid.nativealpha.activities

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.databinding.AdblockConfigActivityBinding
import com.cylonid.nativealpha.databinding.AddAdblockConfigDialogBinding
import com.cylonid.nativealpha.fragments.adblocklist.AdblockListFragment
import com.cylonid.nativealpha.model.AdblockConfig
import com.cylonid.nativealpha.model.DataManager


class AdblockConfigActivity : AppCompatActivity() {
    private lateinit var adblockListFragment: AdblockListFragment
    private lateinit var binding: AdblockConfigActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdblockConfigActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.adblockFab.setOnClickListener { showAddAdblockDialog() }

        adblockListFragment =
            supportFragmentManager.findFragmentById(R.id.adblock_fragment_container_view) as AdblockListFragment
    }

    private fun updateAdblockList() {
        adblockListFragment.updateAdblockList()
    }


    override fun onBackPressed() {
        moveTaskToBack(true)
        super.onBackPressed()
    }

    private fun showAddAdblockDialog() {
        val localBinding = AddAdblockConfigDialogBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(localBinding.root)
            .setTitle(getString(R.string.add_a_new_adblock_provider))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                val url = localBinding.addAdblockUrl.text.toString().trim()
                val formattedUrl =
                    if (url.startsWith("https://") || url.startsWith("http://")) url else "https://$url"

                DataManager.getInstance().apply {
                    val label =
                        if (localBinding.addAdblockLabel.text.isNotEmpty()) localBinding.addAdblockLabel.text.toString() else url
                    settings.globalWebApp.adBlockSettings += AdblockConfig(label, formattedUrl)
                    saveGlobalSettings()
                }
                updateAdblockList()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()

        val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        okButton.isEnabled = false
        localBinding.addAdblockUrl.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                okButton.isEnabled = !s.isNullOrBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }
}


