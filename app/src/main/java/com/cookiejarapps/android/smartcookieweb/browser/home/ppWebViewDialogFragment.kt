package com.cookiejarapps.android.smartcookieweb.browser.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.cookiejarapps.android.smartcookieweb.R

class InAppWebViewDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_URL = "url"
        fun newInstance(url: String): InAppWebViewDialogFragment {
            val frag = InAppWebViewDialogFragment()
            val args = Bundle()
            args.putString(ARG_URL, url)
            frag.arguments = args
            return frag
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_in_app_webview, container, false)
        val webView = view.findViewById<WebView>(R.id.webview)
        webView.settings.javaScriptEnabled = true
        val url = arguments?.getString(ARG_URL) ?: "about:blank"
        webView.loadUrl(url)

        view.findViewById<Button>(R.id.btn_close)?.setOnClickListener {
            dismiss()
        }
        return view
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
